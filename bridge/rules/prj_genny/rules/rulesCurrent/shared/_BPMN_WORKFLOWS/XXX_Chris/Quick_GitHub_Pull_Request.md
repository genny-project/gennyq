# GitHub - Pull Request
---
**1. cd to the right folder where you want to make a pull request**

genny-main (v3.1.0) $ `cd ../prj_genny/`

---

**2. Make sure you are on the right GitHub Branch**

prj_genny (v3.1.0) $ `git branch`

\* v3.1.0

---

**3. Git Pull to get your shadow copy up to date with origin copy**

prj_genny (v3.1.0) $ `git pull`

---

**4. Git staus shows how far my shadow copy is behind the master**

prj_genny (v3.1.0) $ `git status`

---

**5. Git checkout to a new branch to make the pull request from**

prj_genny (v3.1.0) $ `git checkout -b applicationlc`

---

**6. Git add . will add your files here in the new branch**

prj_genny (applicationlc) $ `git add .`

---

**7. Git status will show the changes to the folder you are requesting in the pull request**

prj_genny (applicationlc) $ `git status`

---

**8. Git commit -m "description" will commit your changes to the new branch**

prj_genny (applicationlc) $ `git commit -m "built applicationlc and placementlc"`

---

**9. Actually git push the changes**

prj_genny (applicationlc) $ `git push`

---

**10. Copy and paste the git push command above to create pull request**

prj_genny (applicationlc) $ `git push --set-upstream origin applicationlc`

---

**11. Go to github.com and go to the folder you made the pull rquest to**

---

**12. Click on tab labelled Pull Requests**

---

**13. Click on Compare and Pull Request (Green Button)**

---

**14. Select your reviewers (eg. Adam) and then click the Create Pull Request**