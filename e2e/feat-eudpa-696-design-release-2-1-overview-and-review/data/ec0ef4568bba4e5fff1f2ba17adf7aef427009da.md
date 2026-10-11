# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: plants/e2e/features/review.spec.ts >> High-risk plants check and submit section >> resubmitting an amended arrival date preserves the original late flag
- Location: tests/plants/e2e/features/review.spec.ts:369:5

# Error details

```
Test timeout of 30000ms exceeded.
```

```
Error: locator.click: Test timeout of 30000ms exceeded.
Call log:
  - waiting for getByRole('option', { name: 'Aberdeen Harbour (GB ABD)', exact: true })

```

# Page snapshot

```yaml
- generic [ref=f6e1]:
  - link "Skip to main content" [ref=f6e2] [cursor=pointer]:
    - /url: "#main-content"
  - banner [ref=f6e3]:
    - link [ref=f6e7] [cursor=pointer]:
      - /url: https://www.gov.uk/
      - img "GOV.UK" [ref=f6e8]
    - region "Service information" [ref=f6e21]:
      - generic [ref=f6e23]:
        - link "Import notification service" [ref=f6e25] [cursor=pointer]:
          - /url: /high-risk-plants
        - navigation "Menu" [ref=f6e26]:
          - list [ref=f6e27]:
            - listitem [ref=f6e28]:
              - link [ref=f6e29] [cursor=pointer]:
                - /url: /high-risk-plants
                - strong [ref=f6e30]: Dashboard
            - listitem [ref=f6e31]:
              - link "Address book" [ref=f6e32] [cursor=pointer]:
                - /url: http://localhost:3002/address-book
            - listitem [ref=f6e33]:
              - link "Manage account" [ref=f6e34] [cursor=pointer]:
                - /url: "#"
            - listitem [ref=f6e35]:
              - link "Log out" [ref=f6e36] [cursor=pointer]:
                - /url: /auth/sign-out
  - generic [ref=f6e37]:
    - paragraph [ref=f6e39]:
      - strong [ref=f6e40]: Alpha
      - generic [ref=f6e41]:
        - text: This is a new service. Help us improve it and
        - link "give your feedback by email" [ref=f6e42] [cursor=pointer]:
          - /url: mailto:APHAServiceDesk@apha.gov.uk
        - text: .
    - link "Back" [ref=f6e43] [cursor=pointer]:
      - /url: /high-risk-plants/notifications/GBN-HRP-26-GFQJHM
    - main [ref=f6e44]:
      - generic [ref=f6e46]:
        - generic [ref=f6e47]:
          - strong [ref=f6e48]: Draft
          - text: GBN-HRP-26-GFQJHM
        - generic [ref=f6e49]: Arrival
        - heading "Arrival details" [level=1] [ref=f6e50]
        - generic [ref=f6e51]:
          - generic [ref=f6e53]:
            - generic [ref=f6e54]: Expected date of arrival
            - generic [ref=f6e55]: For example, 27/3/2026. If the potatoes have already arrived, enter the date they arrived.
            - generic [ref=f6e57]:
              - textbox "Expected date of arrival" [ref=f6e58]
              - button "Choose date" [ref=f6e59] [cursor=pointer]
          - generic [ref=f6e65]:
            - generic [ref=f6e66]: Expected time of arrival
            - generic [ref=f6e67]: Use the 24-hour clock. For example, 14:30.
            - textbox "Expected time of arrival" [ref=f6e68]: 14:30
          - generic [ref=f6e69]:
            - generic [ref=f6e70]: Proposed place of landing
            - generic [ref=f6e71]: Select where the potatoes will enter Great Britain. Start typing to search by port or airport name or code.
            - generic [ref=f6e73]:
              - generic [ref=f6e74]:
                - status [ref=f6e75]: No search results
                - status
              - combobox "Proposed place of landing" [active] [ref=f6e76] [cursor=pointer]: Aberdeen Harbour (GB ABD)
              - listbox "Aberdeen Harbour (GB ABD)" [ref=f6e80]:
                - option "No ports found" [disabled] [ref=f6e81]
          - generic [ref=f6e82]:
            - button "Save and continue" [ref=f6e83] [cursor=pointer]
            - button "Save and return to overview" [ref=f6e84] [cursor=pointer]
            - link "Cancel and return to overview" [ref=f6e85] [cursor=pointer]:
              - /url: /high-risk-plants/notifications/GBN-HRP-26-GFQJHM
  - contentinfo [ref=f6e86]:
    - generic [ref=f6e99]:
      - generic [ref=f6e100]:
        - heading "Support links" [level=2] [ref=f6e101]
        - list [ref=f6e102]:
          - listitem [ref=f6e103]:
            - link "Privacy" [ref=f6e104] [cursor=pointer]:
              - /url: https://www.gov.uk/help/privacy-notice
          - listitem [ref=f6e105]:
            - link "Cookies" [ref=f6e106] [cursor=pointer]:
              - /url: https://www.gov.uk/help/cookies
          - listitem [ref=f6e107]:
            - link "Accessibility statement" [ref=f6e108] [cursor=pointer]:
              - /url: https://www.gov.uk/help/accessibility-statement
        - generic [ref=f6e111]:
          - text: All content is available under the
          - link "Open Government Licence v3.0" [ref=f6e112] [cursor=pointer]:
            - /url: https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/
          - text: ", except where otherwise stated"
      - link "© Crown copyright" [ref=f6e114] [cursor=pointer]:
        - /url: https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/uk-government-licensing-framework/crown-copyright/
```

# Test source

```ts
  1  | import { type Locator } from '@playwright/test';
  2  | import { PlantsNotificationPage } from '@page-objects/plants/notification-page';
  3  | 
  4  | /**
  5  |  * When the consignment arrives, and for potatoes what time and where it lands.
  6  |  *
  7  |  * Every commodity type is asked this page, so it is the arrival section's last
  8  |  * step for all of them. The date question means three different things — the
  9  |  * expected date of arrival for potatoes, the expected date of landing before
  10 |  * arrival, the date it first arrived after — and the label is the only thing
  11 |  * that says which, so it is addressed rather than assumed. The time and the
  12 |  * place of landing are potato matters and are not rendered for anything else.
  13 |  */
  14 | export class PlantsArrivalDetailsPage extends PlantsNotificationPage {
  15 |   constructor(page: ConstructorParameters<typeof PlantsNotificationPage>[0]) {
  16 |     super(page, 'arrival-details');
  17 |   }
  18 | 
  19 |   get heading(): Locator {
  20 |     return this.page.getByRole('heading', { level: 1, name: 'Arrival details' });
  21 |   }
  22 | 
  23 |   /** The MoJ date picker is one text box taking `d/m/yyyy`, not three date parts. */
  24 |   get arrivalDate(): Locator {
  25 |     return this.page.locator('#arrivalDate');
  26 |   }
  27 | 
  28 |   /**
  29 |    * The date question addressed by the sentence it is asked under. The label is
  30 |    * the input's accessible name, so naming it both finds the field and proves
  31 |    * which of the three states the page put it in.
  32 |    */
  33 |   dateQuestionLabelled(label: string): Locator {
  34 |     return this.page.getByRole('textbox', { name: label, exact: true });
  35 |   }
  36 | 
  37 |   get arrivalTime(): Locator {
  38 |     return this.page.locator('#arrivalTime');
  39 |   }
  40 | 
  41 |   /**
  42 |    * Proposed place of landing is an accessible-autocomplete type-ahead
  43 |    * enhancing a native <select>, the same enhancement the origin page uses: the
  44 |    * enhanced input takes the select's id, so `#proposedPlaceOfLanding` is
  45 |    * whichever of the two the trader actually types into.
  46 |    */
  47 |   get proposedPlaceOfLanding(): Locator {
  48 |     return this.page.locator('#proposedPlaceOfLanding');
  49 |   }
  50 | 
  51 |   placeOfLandingOption(name: string): Locator {
  52 |     return this.page.getByRole('option', { name, exact: true });
  53 |   }
  54 | 
  55 |   /** Ports are offered as "{name} ({code})", the one label both sides share. */
  56 |   async selectPlaceOfLanding(name: string): Promise<void> {
  57 |     // The enhancement is a module script, so it has run by DOMContentLoaded.
  58 |     // Waiting for that settles which element the id resolves to before the
  59 |     // probe reads it. With JavaScript off the event has already fired.
  60 |     await this.page.waitForLoadState('domcontentloaded');
  61 |     const field = this.proposedPlaceOfLanding;
  62 |     if ((await field.evaluate((el) => el.tagName)) === 'SELECT') {
  63 |       await field.selectOption({ label: name });
  64 |       return;
  65 |     }
  66 |     await field.click();
  67 |     await field.fill(name);
> 68 |     await this.placeOfLandingOption(name).click();
     |                                           ^ Error: locator.click: Test timeout of 30000ms exceeded.
  69 |   }
  70 | 
  71 |   get btnSaveAndContinue(): Locator {
  72 |     return this.page.getByRole('button', { name: 'Save and continue', exact: true });
  73 |   }
  74 | 
  75 |   get btnSaveAndReturnToOverview(): Locator {
  76 |     return this.page.getByRole('button', { name: 'Save and return to overview', exact: true });
  77 |   }
  78 | 
  79 |   /** A plain link, not a govukButton, so it is addressed by the link role. */
  80 |   get linkCancel(): Locator {
  81 |     return this.page.getByRole('link', { name: 'Cancel and return to overview', exact: true });
  82 |   }
  83 | 
  84 |   get errorSummary(): Locator {
  85 |     return this.page.locator('.govuk-error-summary');
  86 |   }
  87 | }
  88 | 
```