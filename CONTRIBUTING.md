Contributor Guidelines
======================

## Sign the contributor license agreement

Very important, before we can accept any Spring Integration contributions, we will need you to sign the S2 contributor license agreement (CLA). Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. In order to read and sign the CLA, please go to:

* [https://support.springsource.com/spring_committer_signup](https://support.springsource.com/spring_committer_signup)

For **Project**, please select **Spring Integration**. The **Project Lead** is **Mark Fisher**.


## Fork the Repository
1. go to [https://github.com/SpringSource/spring-integration](https://github.com/SpringSource/spring-integration)
2. hit the "fork" button and choose your own github account as the target
3. for more detail see [http://help.github.com/fork-a-repo/](http://help.github.com/fork-a-repo/)

## Setup your Local Development Environment
1. `git clone --recursive git@github.com:<your-github-username>/spring-integration.git`
2. `cd spring-integration`
3. `git remote show`
_you should see only 'origin' - which is the fork you created for your own github account_
4. `git remote add upstream git@github.com:SpringSource/spring-integration.git`
5. `git remote show`
_you should now see 'upstream' in addition to 'origin' where 'upstream' is the SpringSource repository from which releases are built_
6. `git fetch --all`
7. `git branch -a`
_you should see branches on origin as well as upstream, including 'master' and 'maint'_

## A Day in the Life of a Contributor
* _Always_ work on topic branches.
* For example, to create and switch to a new branch for issue INT-123: `git checkout -b INT-123`
* You might be working on several different topic branches at any given time, but when at a stopping point for one of those branches, commit (a local operation).
* Please follow the "Commit Guidelines" described in this chapter of Pro Git: [http://progit.org/book/ch5-2.html](http://progit.org/book/ch5-2.html)
* Then to begin working on another issue (say INT-101): `git checkout INT-101`. The _-b_ flag is not needed if that branch already exists in your local repository.
* When ready to resolve an issue or to collaborate with others, you can push your branch to origin (your fork), e.g.: `git push origin INT-123`
* If you want to collaborate with another contributor, have them fork your repository (add it as a remote) and `git fetch <your-username>` to grab your branch. Alternatively, they can use `git fetch --all` to sync their local state with all of their remotes.
* If you grant that collaborator push access to your repository, they can even apply their changes to your branch.
* When ready for your contribution to be reviewed for potential inclusion in the master branch of the canonical spring-integration repository (what you know as 'upstream'), issue a pull request to the SpringSource repository (for more detail, see [http://help.github.com/send-pull-requests/](http://help.github.com/send-pull-requests/)).
* The project lead may merge your changes into the upstream master branch as-is, he may keep the pull request open yet add a comment about something that should be modified, or he might reject the pull request by closing it.
* A prerequisite for any pull request is that it will be cleanly merge-able with the upstream master's current state. **This is the responsibility of any contributor.** If your pull request cannot be applied cleanly, the project lead will most likely add a comment requesting that you make it merge-able. For a full explanation, see the Pro Git section on rebasing: [http://progit.org/book/ch3-6.html](http://progit.org/book/ch3-6.html). As stated there: "> Often, you’ll do this to make sure your commits apply cleanly on a remote branch — perhaps in a project to which you’re trying to contribute but that you don’t maintain."

## Keeping your Local Code in Sync
* As mentioned above, you should always work on topic branches (since 'master' is a moving target). However, you do want to always keep your own 'origin' master branch in synch with the 'upstream' master.
* Within your local working directory, you can sync up all remotes' branches with: `git fetch --all`
* While on your own local master branch: `git pull upstream master` (which is the equivalent of fetching upstream/master and merging that into the branch you are in currently)
* Now that you're in synch, switch to the topic branch where you plan to work, e.g.: `git checkout -b INT-123`
* When you get to a stopping point: `git commit`
* If changes have occurred on the upstream/master while you were working you can synch again:
    - Switch back to master: `git checkout master`
    - Then: `git pull upstream master`
    - Switch back to the topic branch: `git checkout INT-123` (no -b needed since the branch already exists)
    - Rebase the topic branch to minimize the distance between it and your recently synched master branch: `git rebase master`
(Again, for more detail see the Pro Git section on rebasing: [http://progit.org/book/ch3-6.html](http://progit.org/book/ch3-6.html))
* **Note** You cannot rebase if you have already pushed your branch to your remote because you'd be rewriting history (see **'The Perils of Rebasing'** in the article). If you rebase by mistake, you can undo it as discussed [in this stackoverflow discussion](http://stackoverflow.com/questions/134882/undoing-a-git-rebase). Once you have published your branch, you need to merge in the master rather than rebasing.
* Now, if you issue a pull request, it is much more likely to be merged without conflicts. Most likely, any pull request that would produce conflicts will be deferred until the issuer of that pull request makes these adjustments.
* Assuming your pull request is merged into the 'upstream' master, you will actually end up pulling that change into your own master eventually, and at that time, you may decide to delete the topic branch from your local repository and your fork (origin) if you pushed it there.
    - to delete the local branch: `git branch -d INT-123`
    - to delete the branch from your origin: `git push origin :INT-123`
