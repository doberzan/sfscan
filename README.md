# sfscan

This is a Burpsuite extension tool that can be used for Salesforce application testing. This tool is __not__ very stable and I did not refactor my code very much. Some functions may be inefficient, but the important part is that it works. - Sometimes - 

## Functionality
For anything to work you **must** add the salesforce server as a target in your scope. If you do not do this nothing will populate.

### Methods Tab
This tab collects Apex functions that are called when preforming actions on the webpage. The class and function name are saved along with the parameters used in the request. The side panel provides a request overview that can be sent to Repeater.

### Objects Tab
This tab can be used to enumerate objects from the server using the `getConfigData` builtin Salesforce function. This does not work very well right now, but the target domain input should be just the root domain of the Salesforce server Example:`test.example.com`.

### ID Tab
This tab collects Salesforce object IDs it catches going through the Proxy. The regex I used for this is very not great and that is ok.

## Targets Tab
This tab I added so I could easily copy paste the active scope links - It has nothing to do with Salesforce :)
