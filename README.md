# AMT Altinn ACL

## Legge til testdata

For å legge til permanent testdata i cachen for å ikke gjøre oppslag mot Altinn så
kan man kjøre følgende SQL:

```sql
insert into rettigheter_cache(norsk_ident, data_version, expires_after, data_json)
values ('<fnr>', 2, to_timestamp('3000-01-01', 'YYYY-MM-DD'),
        '{
          "version": 1,
          "rettigheter": [
            {
              "serviceCode": "9999999",
              "organisasjonsnummer": "<orgnr>"
            }
          ]
        }')
on conflict (norsk_ident)
    DO UPDATE SET data_json = EXCLUDED.data_json,
                  data_version = EXCLUDED.data_version,
                  expires_after = EXCLUDED.expires_after;
```