import type { CountryCodeRule, CountryOption } from '@/types'

export interface SupportedCountry extends CountryOption {
  countryCode: string
  localPhoneLength: number
}

export const supportedCountries: readonly SupportedCountry[] = [
  { code: 'NG', name: 'Nigeria', countryCode: '+234', localPhoneLength: 10 },
  { code: 'IN', name: 'India', countryCode: '+91', localPhoneLength: 10 },
  { code: 'CM', name: 'Cameroon', countryCode: '+237', localPhoneLength: 9 },
  { code: 'GH', name: 'Ghana', countryCode: '+233', localPhoneLength: 9 },
  { code: 'KE', name: 'Kenya', countryCode: '+254', localPhoneLength: 9 }
]

export const supportedCountryOptions: CountryOption[] = supportedCountries.map(({ code, name }) => ({ code, name }))

export const fallbackCountryCodeRules: CountryCodeRule[] = supportedCountries.map((country, index) => ({
  countryCode: country.countryCode,
  countryName: country.name,
  minLocalLength: country.localPhoneLength,
  maxLocalLength: country.localPhoneLength,
  enabled: true,
  sortOrder: (index + 1) * 10
}))
