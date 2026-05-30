export interface RealEstateListingSchema {
  "@context": "https://schema.org";
  "@type": "RealEstateListing";
  name: string;
  description: string;
  url: string;
  image?: string[];
  datePosted?: string;
  availability?: string;
  price?: string;
  priceCurrency?: string;
  address?: {
    "@type": "PostalAddress";
    streetAddress?: string;
    addressLocality: string;
    addressRegion?: string;
    addressCountry: string;
  };
  numberOfRooms?: number;
  floorSize?: {
    "@type": "QuantitativeValue";
    value: number;
    unitCode: string;
  };
  amenityFeature?: Array<{
    "@type": "LocationFeatureSpecification";
    name: string;
    value: boolean;
  }>;
}

export interface ProductSchema {
  "@context": "https://schema.org";
  "@type": "Product";
  name: string;
  description: string;
  image?: string[];
  url?: string;
  brand?: {
    "@type": "Brand";
    name: string;
  };
  offers?: {
    "@type": "Offer";
    price: string;
    priceCurrency: string;
    availability: string;
    url?: string;
  };
}

export interface LocalBusinessSchema {
  "@context": "https://schema.org";
  "@type": "LocalBusiness";
  name: string;
  description: string;
  url: string;
  image?: string;
  address?: {
    "@type": "PostalAddress";
    addressLocality: string;
    addressRegion?: string;
    addressCountry: string;
  };
  priceRange?: string;
  areaServed?: string;
}

export type SchemaType = RealEstateListingSchema | ProductSchema | LocalBusinessSchema;

export function serializeSchema(schema: SchemaType): string {
  return JSON.stringify(schema);
}
