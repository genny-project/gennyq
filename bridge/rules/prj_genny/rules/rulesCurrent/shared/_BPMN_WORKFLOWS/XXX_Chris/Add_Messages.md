# How to add a message to the automatic system

## Editing the sheets
#### 1. Open the 'Notifications' tab in the Internmatch google doc 

`https://docs.google.com/spreadsheets/d/1u5CgISIqhKPU2TXmgfthvwNnhKrhIvmjec8vFbkKRhc/edit#gid=401616936`

#### 2. Copy the email data into the html viewer

`https://htmledit.squarefree.com/`

#### 3. Open the 'Notifications' tab in the Genny google doc 

`https://docs.google.com/spreadsheets/d/1n60kJeBGY4v084JnhZtAxW-V1dnK9yNzjAs5qnDpd2k/edit#gid=791052034`

#### 4. Fill in the details including email, sms and toast

Remember: Cannot have a balnk field. Insert a dash (-) if supposed to be blank.

---

## Batch load the new data into the database
#### 5. Stop genny-main

`./stop.sh`

#### 6. Run genny-main

`./run.sh dev1 up`

#### 7. Monitor the logs in a new terminal 

`./logs.sh`

---

## Check the database
#### 8. Open 'Sequel Pro'

https://sequelpro.com/download

**Name**: Genny DB <br/>
**Host**: 127.0.0.1 <br/>
**Username**: genny <br/> 
**Password**: password <br/>
**Database**: gennydb <br/>
**Port**: 3310 <br/>

#### 9. Open 'template db' to see your new entry

---

## Run the workflow
#### 10. Ensure notificationTemplate is set to the 'code' as per the Genny google doc

