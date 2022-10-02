# Branch Manager Flows

### Action: 
User opens URL "http://localhost:4545" in a browser

*Flow*:
* Check if there is a cookie with the SVN username and SVN password.
    * If not open dialog to enter SVN Username and Password and write in a cookie
* Check the SVN status of the WC that is in the folder defined by the property "workdir"
    * If there are non-committed files show the "Commit first" page
    * User presses "Commit"
* Check if the branch has merged with the latest Trunk
    * If so, show message "Branch is up-to-date" and disable the "Merge Trunk flows into branch" button
* Check if the are UI pages present
    * If so, enable the "Merge Trunk UI into branch" button
* If "Merge Trunk flows into branch" button AND "Merge Trunk UI into branch" buttons are disbled, disable the "Update Branch" button also
* Show the main page

### Action:
User presses the "Create new Branch" button

*Flow*:
* 

### Action:
User presses the "Merge Trunk Flows into Branch" button

*Flow*:
* 

### Action:
User presses the "Merge Trunk UI into Branch" button

*Flow*:
* 

### Action:
User presses the "Update Branch" button

*Flow*:
* 

### Action:
User presses the "Remove Branches" button

*Flow*:
* 
