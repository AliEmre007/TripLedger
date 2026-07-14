alter table organisation
    add constraint organisation_base_currency_supported check (
        base_currency in ('EUR', 'GBP', 'TRY', 'USD')
    );

alter table booking
    add constraint booking_selling_currency_supported check (
        selling_currency in ('EUR', 'GBP', 'TRY', 'USD')
    ),
    add constraint booking_contracted_selling_amount_minor_unit check (
        contracted_selling_amount = round(contracted_selling_amount, 2)
    );

alter table booking_item
    add constraint booking_item_selling_currency_supported check (
        selling_currency in ('EUR', 'GBP', 'TRY', 'USD')
    ),
    add constraint booking_item_selling_amount_minor_unit check (
        selling_amount = round(selling_amount, 2)
    );

alter table supplier_obligation
    add constraint supplier_obligation_currency_supported check (
        currency in ('EUR', 'GBP', 'TRY', 'USD')
    ),
    add constraint supplier_obligation_amount_minor_unit check (
        amount = round(amount, 2)
    );

alter table financial_event
    add constraint financial_event_currency_supported check (
        currency in ('EUR', 'GBP', 'TRY', 'USD')
    ),
    add constraint financial_event_amount_minor_unit check (
        amount = round(amount, 2)
    );
