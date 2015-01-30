# Synapse Client for Cytoscape

For details on what this app does, check out the app page: http://apps.cytoscape.org/apps/synapseclient

## Key classes

* **SynClient.java**

    Issues requests to the Synapse web service. Requests are encapsulated as Cytoscape tasks.
    
    This class does not use the Java client library provided by Synapse, as it doesn't provide the means
    to cancel HTTP requests nor the means to monitor the progress of requests. These limitations prevent their
    client library from being able to be tied into Cytoscape's task structure. This class addresses these
    limitations and acts as a replacement for the client Java library provided by Synapse.

* **BrowserDialog.java**

    This is the main Swing UI dialog for this app.

    Most tasks have the static method `noTunables`. This method is used by `BrowserDialog`. For instance,
    `ImportNetworkFromSynapseTask` has a tunable for the Synapse entity ID. In the browser dialog, 
    when the user selects an entity, the `BrowserDialog` calls `noTunables`, as it already knows the entity's
    ID.
    

## How authentication works

1. The user goes to the *Apps* menu and chooses **Synapse**. This starts the `BrowseTask`.
1. `SynClientMgr` maintains an instance of `SynClient`.
The `BrowserTask` sees if there's a valid `SynClient` instance. If there is, the user
is already logged in and proceeds to open the browser dialog.
1. If there is no valid `SynClient` instance, the user isn't logged in. The `BrowseTask`
opens the login dialog.
1. When the user clicks *OK* in the login dialog, `BrowseTask` creates an instance of `SynClient` using
a new `APIAuthKey` with the provided user credentials. It then calls `SynClient.newUserProfileTask` and runs
the task.
1. If the task succeeds, the user's credentials are correct. It registers the new instance of `SynClient` with
`SynClientMgr`. This closes the login dialog and opens the browser dialog.
1. If the task fails, the login dialog shows an error message.

## How the entity types are detected

When the user selects an entity in the browser dialog, the browser dialog determines whether the entity can be
imported as a network, table, or session. How does this work?

`ImporterMgr` maintains a collection of all `InputStreamTaskFactory`s registered in OSGi. It extracts each
`InputStreamTaskFactory`'s `CyFileFilter`, which specifies the file types supported by a given
`InputStreamTaskFactory`. `BrowserDialog` calls `ImporterMgr.doesImporterExist` with the entity's extension
and either `NETWORK`, `TABLE`, or `SESSION` for `DataCategory`. If `ImporterMgr` finds an `InputStreamFactory`
that supports the given file extension and `DataCategory`, it will return true. The browser dialog will then
add an import button for the given `DataCategory`.
