#!/bin/bash
# see https://help.github.com/articles/syncing-a-fork/
echo "Updating this GIT to original:"
echo ""
git remote add upstream https://github.com/openimaj/openimaj.git
git fetch upstream
git checkout master
git merge upstream/master
#git pull upstream master
git push origin master
echo "push to yout fork's remote"
read -rsp $'Done. Press any key to quit...\n' -n1 key
