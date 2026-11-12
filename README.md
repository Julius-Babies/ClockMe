# ClockMe

ClockMe is a small application to get a time tracking-like overview of your Android calendars.

I use it to track how much time I spend in different classes at university and how much time I spend
there in general.

## AI Disclaimer
As this is mostly a quickly wanted application for my personal needs, Claude is used to write most
of the code, I only guide it into the architecture and structure of the app. Since this app does
not connect to the internet, nor has it write access to your calendar, AI-generated code shouldn't
be a security risk.

## Ideal setup
This app makes use of the Android shared calendar architecture: Contacts and Calendar are stored
globally on Android and apps can request access. If you sync your Nextcloud calendar for example via
an app like DavX5, every calendar on your nextcloud instance is cloned into a new local-calendar,
meaning every app could view it. ClockMe splits time tracking accross calendars, so ideally you would
have one calendar per activity. I have one calendar for studying in general and one for each class
where I save lectures or homework focus time. Other calendars are used per hobby-activity as well as
one for my work so I can generate a report at the end of the month.