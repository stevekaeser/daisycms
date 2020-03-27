The emailnotifier service provides the feature of being able
to send emails to users when certain events occur in the repository.

This consists of two parts:

 1. being able to manage the subscription options for users (do they want
    emails, and of what events)

 2. reacting to the actual events, and based on subscription information,
    send out the emails.

This component doesn't provide direct services to other components, rather
it registers an "EmailSubscriptionManager" extension with the repository.
This extension is available both server-side and client-side.

While this component needs a database to manage the subscription information,
this database doesn't need to be the same as the core repository database.

Cleanup of subscriptions of deleted users: if users get deleted from the system,
their subscription information will be cleaned up the next time the system
tries to use their subscription.

Handling of changes in the roles to which a user belongs: if the user does
not longer hold the role used in his/her subscription, the system will
fall back to the users's default role.