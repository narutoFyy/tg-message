import type { CountryCodeRule, CountryOption } from '@/types'

export interface SupportedCountry extends CountryOption {
  countryCode: string
  currencyCode: string
  currencySymbol: string
  localPhoneLength: number
}

export const supportedCountries: readonly SupportedCountry[] = [
  { code: 'NG', name: 'Nigeria', countryCode: '+234', currencyCode: 'NGN', currencySymbol: '₦', localPhoneLength: 10 },
  { code: 'IN', name: 'India', countryCode: '+91', currencyCode: 'INR', currencySymbol: '₹', localPhoneLength: 10 },
  { code: 'CM', name: 'Cameroon', countryCode: '+237', currencyCode: 'XAF', currencySymbol: 'FCFA', localPhoneLength: 9 },
  { code: 'GH', name: 'Ghana', countryCode: '+233', currencyCode: 'GHS', currencySymbol: 'GH₵', localPhoneLength: 9 },
  { code: 'KE', name: 'Kenya', countryCode: '+254', currencyCode: 'KES', currencySymbol: 'KSh', localPhoneLength: 9 },
  { code: 'US', name: 'United States', countryCode: '+1', currencyCode: 'USD', currencySymbol: '$', localPhoneLength: 10 }
]

export const supportedCountryOptions: CountryOption[] = supportedCountries.map(({ code, name }) => ({ code, name }))

export const fallbackCountryCodeRules: CountryCodeRule[] = supportedCountries.map((country, index) => ({
  code: country.code,
  countryCode: country.countryCode,
  countryName: country.name,
  currencyCode: country.currencyCode,
  currencySymbol: country.currencySymbol,
  minLocalLength: country.localPhoneLength,
  maxLocalLength: country.localPhoneLength,
  enabled: true,
  sortOrder: (index + 1) * 10
}))
