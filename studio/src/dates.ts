const options = { year: 'numeric', month: 'long', day: 'numeric' } as Intl.DateTimeFormatOptions
export const dateTimeFormatter = new Intl.DateTimeFormat('en-US', options)
