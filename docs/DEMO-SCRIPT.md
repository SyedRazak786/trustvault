# Five-minute demo script

1. **(0:00 to 0:30) Introduction.** State the problem: the wrong person reads a file, and nobody can say who opened it.
2. **(0:30 to 1:30) Owner uploads and grants.** Log in as the owner. Upload a salary letter. Grant READ to the viewer account.
3. **(1:30 to 2:30) Viewer reads.** Log in as the viewer. Show the document in the list and download it. Show a second, ungranted document is absent from the list and returns 404 when its ID is typed in directly.
4. **(2:30 to 3:30) Revocation.** Owner revokes the grant. Viewer refreshes and gets 404 on the same download, immediately.
5. **(3:30 to 4:30) Audit.** Admin opens the activity view. Show the DENIED attempt, the download, and the revocation. Run the hash verification.
6. **(4:30 to 5:00) Code.** Open `AccessService.require`. Show that every route calls it. Remove the call, run the test, show it fail, and restore it.

Practise the code step until the removal and test run takes under a minute.
