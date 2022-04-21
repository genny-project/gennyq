# Notification Workflows

This workflows will accept sigal reqeust then sends email or SMS to the userSession owner.

- The workflow extracts the user information such as email, name, mobile.
- The workflows will combine the student info with a template, then pass the message to Email API or SMS API.
- The template engine will us FreeMaker
- FreeMaker Eclipse plugin : https://marketplace.eclipse.org/content/freemarker-ide
- FreeMaker sandbox: https://try.freemarker.apache.org/
- FreeMaker Examples: https://freemarker.apache.org/docs/pgui_quickstart_all.html
- The package will use QMessage Services genny/messages/src/main/java/life/genny/message/QEmailMessageManager.java and genny/message/QSMSMessageManager.java
- genny/qwanda-utils/src/main/java/life/genny/qwandautils/MessageUtils.java
- genny-rules/src/main/java/life/genny/rules/QRules.java line:923 public void sendMessage()
- Send email exmaple projects/genny/prj_internmatch/rules/05_Workflows/InternPlacement/SendEmailWithAttachment/00_TriggerGeneratingWeeklyJournal.drl
- prj_internmatch/rules/05_Workflows/InternPlacement/SendEmailWithAttachment/20_Send_Email_With_Attachment.drl



How to use the Notifcation Hub workflow?

1. In the workflows property set data item notificaitonCode as String
2. In the Call Activity, kcontext.setVariable("notificationCode", "APPLIED");
3. In the I/O Parameteres, Input Data Mapping, From "notificationCode" to "notificationCode".



Signal `controlSignal` that triggers action:
1. SHORTLIST
2. INTERVIEW
3. OFFER
4. PLACED
5. IN_PROGRESS
6. FINISH_INTERNSHIP