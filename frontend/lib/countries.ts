// Country codes with flags and dial codes
export interface Country {
  code: string;
  name: string;
  dialCode: string;
  flag: string;
}

export const countries: Country[] = [
  { code: "CM", name: "Cameroon", dialCode: "+237", flag: "🇨🇲" },
  { code: "NG", name: "Nigeria", dialCode: "+234", flag: "🇳🇬" },
  { code: "GH", name: "Ghana", dialCode: "+233", flag: "🇬🇭" },
  { code: "KE", name: "Kenya", dialCode: "+254", flag: "🇰🇪" },
  { code: "ZA", name: "South Africa", dialCode: "+27", flag: "🇿🇦" },
  { code: "EG", name: "Egypt", dialCode: "+20", flag: "🇪🇬" },
  { code: "TZ", name: "Tanzania", dialCode: "+255", flag: "🇹🇿" },
  { code: "UG", name: "Uganda", dialCode: "+256", flag: "🇺🇬" },
  { code: "ET", name: "Ethiopia", dialCode: "+251", flag: "🇪🇹" },
  { code: "SN", name: "Senegal", dialCode: "+221", flag: "🇸🇳" },
  { code: "CI", name: "Ivory Coast", dialCode: "+225", flag: "🇨🇮" },
  { code: "BF", name: "Burkina Faso", dialCode: "+226", flag: "🇧🇫" },
  { code: "ML", name: "Mali", dialCode: "+223", flag: "🇲🇱" },
  { code: "NE", name: "Niger", dialCode: "+227", flag: "🇳🇪" },
  { code: "TD", name: "Chad", dialCode: "+235", flag: "🇹🇩" },
  { code: "CF", name: "Central African Republic", dialCode: "+236", flag: "🇨🇫" },
  { code: "GA", name: "Gabon", dialCode: "+241", flag: "🇬🇦" },
  { code: "CG", name: "Congo", dialCode: "+242", flag: "🇨🇬" },
  { code: "CD", name: "DR Congo", dialCode: "+243", flag: "🇨🇩" },
  { code: "AO", name: "Angola", dialCode: "+244", flag: "🇦🇴" },
  { code: "US", name: "United States", dialCode: "+1", flag: "🇺🇸" },
  { code: "GB", name: "United Kingdom", dialCode: "+44", flag: "🇬🇧" },
  { code: "FR", name: "France", dialCode: "+33", flag: "🇫🇷" },
  { code: "DE", name: "Germany", dialCode: "+49", flag: "🇩🇪" },
  { code: "CA", name: "Canada", dialCode: "+1", flag: "🇨🇦" },
  { code: "AU", name: "Australia", dialCode: "+61", flag: "🇦🇺" },
  { code: "IN", name: "India", dialCode: "+91", flag: "🇮🇳" },
  { code: "CN", name: "China", dialCode: "+86", flag: "🇨🇳" },
  { code: "BR", name: "Brazil", dialCode: "+55", flag: "🇧🇷" },
  { code: "MX", name: "Mexico", dialCode: "+52", flag: "🇲🇽" },
];

// Get country by dial code
export function getCountryByDialCode(dialCode: string): Country | undefined {
  return countries.find(c => c.dialCode === dialCode);
}

// Get default country (Cameroon)
export function getDefaultCountry(): Country {
  return countries.find(c => c.code === "CM") || countries[0];
}

