Customers have a need to:
* clone the atscale-gatling repo
* make changes and commit them to their cloned private repo
* still track and merge changes from the main atscale-gatling repo to their private repo

Customers should avoid reinitializing the `.git` folder.

Instead, they can:

**Clone the public repo.** -

**Create a new private repo.** -

**Change the `origin` remote to point to their private repo:

**    ```sh   git remote set-url origin git@github.com:customer-org/private-repo.git   ```

**Push to their private repo.**

**Add the public repo as `upstream`** (as above) to keep pulling updates.

This preserves the full history and makes syncing with the parent project easier.



Here is a step by step approach:

From the directory you want as your parent directory enter: git clone https://github.com/AtScaleInc/atscale-gatling.git
change directories to the new subdirectory atscale-gatling

Using Git, create a company repository (private) let's assume your company is acme
after creating the atscale-gatling repository in your organization (acme)

git remote set-url origin git@github.com:acme/atscale-gatling.git

git remote add upstream https://github.com/AtScaleInc/atscale-gatling.git

We can think of this as creating two tags origin and upstream that point to different repositories.

git fetch upstream
From https://github.com/AtScaleInc/atscale-gatling
* [new branch]      development-1.6 -> upstream/development-1.6
* [new branch]      main            -> upstream/main
* [new branch]      release-1.1     -> upstream/release-1.1
* [new branch]      release-1.2     -> upstream/release-1.2
* [new branch]      release-1.3     -> upstream/release-1.3
* [new branch]      release-1.4     -> upstream/release-1.4
* [new branch]      release-1.5     -> upstream/release-1.5

git checkout -b development-1.6
Switched to new branch 'development-1.6'


run git add to add new or updated content
run git commit to commit the content


git push origin development-1.6
Total 0 (delta 0), reused 0 (delta 0), pack-reused 0
remote:
remote: Create a pull request for 'development-1.6' on GitHub by visiting:
remote:      https://github.com/acme/atscale_gatling/pull/new/development-1.6
remote:
To github.com:acme/atscale_gatling.git
* [new branch]      development-1.6 -> development-1.6


run git fetch upstream to get latest changes from AtScale

check out the branch of interest....   
git checkout release-1.6

merge the changes from upstream/release-1.6 into your local development-1.6 branch
git merge upstream/release-1.6 