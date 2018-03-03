import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.logging.Logger;

public class TelegramBot extends TelegramLongPollingBot {
    private static Logger log = Logger.getLogger(TelegramBot.class.getName());
    private static Boolean needLog = true;
    private static String username = "";
    private static String token = "";
    private static NodeList childrenTime;
    private static NodeList childrenTable;


    private static void initTimetables() {
        try {
            // Создается построитель документа
            DocumentBuilder documentBuilderTime = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            DocumentBuilder documentBuilderTable = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            // Создается дерево DOM документа из файла
            Document time = documentBuilderTime.parse(new File("rt.xml"));
            Document table = documentBuilderTable.parse(new File("sub.xml"));
            Element rootTable = table.getDocumentElement(); //<table>
            childrenTable = rootTable.getChildNodes();
            Element rootTime = time.getDocumentElement(); //<time>
            childrenTime = rootTime.getChildNodes();
            if (needLog) log.info("initTables");
        } catch (FileNotFoundException e) {
            if (needLog) log.warning("file not found");
            System.exit(1);
        } catch (NullPointerException e) {
            if (needLog) log.warning("null ptr");
            System.exit(1);
        } catch (ParserConfigurationException e) {
            e.printStackTrace(System.out);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }


    private static void testTable() {
        String tableString;
        Node week;  //<week>
        NodeList daysOfWeek;
        Node day;  //<day>
        NodeList subjectsList;
        Node subject;   //<sub>
        NodeList subjectInfo;
        Node number;
        Node name;
        Node room;
        Node prof;

        week = childrenTable.item(1);
        daysOfWeek = week.getChildNodes();

        for (Integer j = 1; j <= 5; j++) {
            tableString = "\n\n\n" + j + ":";
            day = daysOfWeek.item((j * 2) - 1);
            subjectsList = day.getChildNodes();
            for (Integer i = 0; i < subjectsList.getLength() - 1; i++) {
                i = i + 1;
                subject = subjectsList.item(i);
                subjectInfo = subject.getChildNodes();
                number = subjectInfo.item(1);
                name = subjectInfo.item(3);
                room = subjectInfo.item(5);
                prof = subjectInfo.item(7);

                Integer k = subjectInfo.getLength();
                if (number.getTextContent().equals(" ")) {
                    continue;
                } else {
                    tableString = tableString + "\nпара:" + number.getTextContent();
                }
                if (name.getTextContent().equals(" ")) {
                    tableString = tableString + "\n-";
                    continue;
                } else {
                    tableString = tableString + "\n" + name.getTextContent();
                }
                if (!room.getTextContent().equals(" ")) {
                    tableString = tableString + "\n" + room.getTextContent();
                }
                if (!prof.getTextContent().equals(" ")) {
                    tableString = tableString + "\n" + prof.getTextContent() + "\n";
                }
                //log.info("si str"+tableString);
            }
            log.info(tableString);
        }
    }

    public static void main(String[] args) {
        initTimetables();
        //testTable();
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new TelegramBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        if (needLog) log.info("main");
    }


    private void initConnectionData() {
        try {
            if (username.isEmpty() || token.isEmpty()) {
                // Создается построитель документа
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                // Создается дерево DOM документа из файла
                Document document = documentBuilder.parse(new File("impData.xml"));

                username = document.getElementsByTagName("username").item(0).getTextContent();
                token = document.getElementsByTagName("token").item(0).getTextContent();
                if (needLog) log.info("init username:" + username + " token:" + token);
            }
        } catch (FileNotFoundException e) {
            if (needLog) log.warning("file not found");
            System.exit(1);
        } catch (NullPointerException e) {
            if (needLog) log.warning("null ptr");
            System.exit(1);
        } catch (ParserConfigurationException e) {
            e.printStackTrace(System.out);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        if (needLog) log.info("getUs");
        initConnectionData();
        return username;
    }


    @Override
    public String getBotToken() {
        if (needLog) log.info("getTok");
        initConnectionData();
        return token;
    }


    private static String getInstTable(LocalDateTime now, Integer numberOfWeek) {
        String timeString = "";
        Node period;
        NodeList periodTimes;
        String tableBegin;
        String tableEnd;

        //разбираем xml файл с расписанием звонков
        for (Integer i = 0; i < childrenTime.getLength() - 1; i++) {
            i = i + 1;
            period = childrenTable.item(i);
            periodTimes = period.getChildNodes();
            tableBegin = periodTimes.item(3).getTextContent();
            tableEnd = periodTimes.item(5).getTextContent();


            //идет ли в настоящее время какая-либо пара
                log.info("сейчас ");
                timeString = periodTimes.item(1).getTextContent() + "*пара" + "*\nначало:" + periodTimes.item(3).getTextContent() +
                        "\nконец:" + periodTimes.item(5).getTextContent() + "\nперемена:" + periodTimes.item(7).getTextContent() + "\n\n";
            //} else timeString = "неучебное время, отдыхай пока";
            if (needLog) log.info("tut" + timeString);
        }
        return timeString;
    }


    private static String getTodaysTable(LocalDateTime thisDay, Integer thisWeek) {
        String tableString = "";
        try {
            Node week = childrenTable.item((thisWeek * 2) - 1); //<week>
            NodeList daysOfWeek = week.getChildNodes();
            Integer numberOfDay = thisDay.getDayOfWeek().getValue();
            if ((numberOfDay == 6) || (numberOfDay == 7)) {
                tableString = "Сегодня выходной день - пар нет!";
                return tableString;
            }

            Node day = daysOfWeek.item((numberOfDay * 2) - 1);  //<day>
            NodeList subjectsList = day.getChildNodes();
            Node subject;   //<sub>
            NodeList subjectInfo;

            Node period; //<period>
            NodeList periodTimes;

            Node number;
            Node name;
            Node room;
            Node prof;
            Integer numberOfSub;
            for (Integer i = 1; i <= 7; i++) {
                numberOfSub = (i * 2) - 1;
                subject = subjectsList.item(numberOfSub);
                subjectInfo = subject.getChildNodes();
                number = subjectInfo.item(1);
                name = subjectInfo.item(3);
                room = subjectInfo.item(5);
                prof = subjectInfo.item(7);

                period = childrenTime.item(numberOfSub);
                periodTimes = period.getChildNodes();


                if (number.getTextContent().equals(" ")) {
                    continue;
                } else {
                    tableString = tableString + "\n*" + number.getTextContent() + " пара*" +
                            "   `" + periodTimes.item(3).getTextContent() + " - " + periodTimes.item(5).getTextContent()+"`";
                }
                if (name.getTextContent().equals(" ")) {
                    tableString = tableString + "\n";
                    continue;
                } else {
                    tableString = tableString + "\n" + name.getTextContent();
                }
                if (!room.getTextContent().equals(" ")) {
                    tableString = tableString + "\n" + room.getTextContent();
                }
                else {
                    tableString = tableString + "\n";
                }
                if (!prof.getTextContent().equals(" ")) {
                    tableString = tableString + "\n" + prof.getTextContent() + "\n";
                }
            }
        }catch(NullPointerException e){
            log.warning("null ptr in getTodaysTable");
            System.exit(1);
        }
        return tableString;
    }



    // То, что выполняется при получении сообщения
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            Calendar dayOfCalendar = Calendar.getInstance();
            LocalDateTime now =  LocalDateTime.now();
            LocalDateTime nowTest = now.plusDays(3);
            Integer k = nowTest.getDayOfWeek().getValue();
            if(needLog) log.info(k.toString());
            Integer numberOfWeek;
            if ((Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2) == 0) {numberOfWeek = 1; }
            else {numberOfWeek = 2;}

            switch (message.getText()){
                case "/help":
                    sendMsg(chatId, "У меня можно узнать расписание.\n/time - расписание звонков" +
                            "\n/today - расписание пар на сегодня" +
                            "\n/week - расписание на эту неделю" +
                            "\n/full - полное расписание на обе недели");
                    break;
                case "/start":
                    sendMsg(chatId, "sendMeTheTimetable бот приветствует. Вот список того, что я могу:" +
                            "\n/time - расписание звонков" +
                            "\n/today - расписание пар на сегодня" +
                            "\n/week - расписание на эту неделю" +
                            "\n/full - полное расписание на обе недели" +
                            "\n/help - список доступных команд");
                    break;
                case "/time":
                    sendImageFromUrl(chatId);
                    break;
                case "/today":
                    String answer = getTodaysTable(nowTest, numberOfWeek);
                    sendMsg(chatId,answer);
                    break;
                case "/week":
                    sendMsg(chatId,"В разработке:(");
                    break;
                case "/full":
                    sendMsg(chatId,"В разработке:(");
                    break;
                default:sendMsg(chatId, "Я не знаю что ответить на это");
            }
        }
    }


    //@SuppressWarnings("deprecation")
    private void sendMsg(String chatId, String text) {
        SendMessage s = new SendMessage();
        s.enableMarkdown(true);
        s.setChatId(chatId);
        //s.setReplyToMessageId(message.getMessageId());
        s.setText(text);
        try {
            // sendMessage(s);
            execute(s);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendImageFromUrl(String chatId) {
        String url = "https://drive.google.com/open?id=1MYpIJRR8qKBIFkRYdCfWuT318Sb50PB_";
        // Create send method
        SendPhoto sendPhotoRequest = new SendPhoto();
        // Set destination chat id
        sendPhotoRequest.setChatId(chatId);
        // Set the photo url as a simple photo
        sendPhotoRequest.setPhoto(url);
        try {
            // Execute the method
            sendPhoto(sendPhotoRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
