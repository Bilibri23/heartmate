import { NextRequest, NextResponse } from 'next/server';

// Backend is on port 8082 (see application.properties server.port=8082)
// Use NEXT_PUBLIC_BACKEND_URL if set, otherwise default to 8082
const BACKEND_URL = process.env.BACKEND_URL || 
  (process.env.NEXT_PUBLIC_BACKEND_URL?.replace('/api', '') || 'http://localhost:8082');

async function handler(request: NextRequest) {
  const path = request.nextUrl.pathname.replace('/api', '');
  const searchParams = request.nextUrl.search;
  const url = `${BACKEND_URL}/api${path}${searchParams}`;

  const headers: HeadersInit = {};

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

    const data = await response.text();
    
    return new NextResponse(data, {
      status: response.status,
      statusText: response.statusText,
      headers: {
        'Content-Type': response.headers.get('Content-Type') || 'application/json',
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
