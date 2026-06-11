import { NextRequest, NextResponse } from 'next/server';
import { springBackendOrigin } from '@/lib/spring-backend-origin';

async function handler(request: NextRequest) {
  const path = request.nextUrl.pathname.replace('/api', '');
  const searchParams = request.nextUrl.search;
  const origin = springBackendOrigin();
  const url = `${origin}/api${path}${searchParams}`;

  if (process.env.NODE_ENV === 'development' && path === '/auth/login' && request.method === 'POST') {
    console.info('[api proxy] POST /api/auth/login → Spring at', url, '(set BACKEND_INTERNAL_URL=http://127.0.0.1:8082 in frontend/.env.local if this is wrong)');
  }

  const requestAccept = request.headers.get('Accept');
  const headers: Record<string, string> = {
    Accept: requestAccept || 'application/json',
  };

  // Forward authorization header if present
  const authHeader = request.headers.get('Authorization');
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }
  
  // Check if this is a multipart request (file upload)
  const contentType = request.headers.get('Content-Type');
  const isMultipart = contentType?.includes('multipart/form-data');

  try {
    let body: BodyInit | undefined;
    
    if (request.method !== 'GET' && request.method !== 'HEAD') {
      if (isMultipart) {
        // For multipart requests, get the formData and reconstruct it
        const incomingFormData = await request.formData();
        const outgoingFormData = new FormData();
        
        for (const [key, value] of incomingFormData.entries()) {
          outgoingFormData.append(key, value);
        }
        
        body = outgoingFormData;
        // Don't set Content-Type - fetch will set it with correct boundary
      } else {
        // For JSON/text requests
        body = await request.text();
        if (contentType) {
          headers['Content-Type'] = contentType;
        } else {
          headers['Content-Type'] = 'application/json';
        }
      }
    }

    const response = await fetch(url, {
      method: request.method,
      headers,
      body,
    });

    const responseContentType = response.headers.get('Content-Type') || 'application/json';
    if (responseContentType.includes('text/event-stream')) {
      return new NextResponse(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers: {
          'Content-Type': responseContentType,
          'Cache-Control': 'no-cache, no-transform',
          Connection: 'keep-alive',
        },
      });
    }

    const data = await response.text();

    if (process.env.NODE_ENV === 'development' && response.status >= 400) {
      const preview = data.length > 1500 ? `${data.slice(0, 1500)}…` : data;
      console.error('[api proxy]', request.method, url, '→', response.status, preview);
    }

    return new NextResponse(data, {
      status: response.status,
      statusText: response.statusText,
      headers: {
        'Content-Type': responseContentType,
      },
    });
  } catch (error) {
    console.error('Proxy error:', error);
    return NextResponse.json(
      { error: 'Failed to connect to backend server' },
      { status: 502 }
    );
  }
}

export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const DELETE = handler;
export const PATCH = handler;
