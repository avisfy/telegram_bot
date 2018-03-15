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
import java.util.Calendar;
import java.util.Timer;
import java.util.logging.Logger;

import static java.lang.Character.getName;

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


    public static void main(String[] args) {
        initTimetables();
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


    private static String getWeekTable(Integer thisWeek) {
        String tableString = "Неделя "+thisWeek+"\n";
        for (Integer j = 1; j <= 5; j++) {
            if (j != 1) tableString = tableString + "------------------------------------------------------------\n";
            tableString = tableString + "\n*" + getName(j) + "*\n" + getTodayTable(j+1 , thisWeek);
        }
        return tableString;
    }


    private static String getTodayTable(Integer thisDay, Integer thisWeek) {
        String tableString = new String();
        try {
            Node week = childrenTable.item((thisWeek * 2) - 1); //<week>
            NodeList daysOfWeek = week.getChildNodes();
            if ((thisDay == Calendar.SATURDAY) || (thisDay == Calendar.SUNDAY)) {
                tableString = "Сегодня выходной день - пар нет!";
                return tableString;
            }
            //вычитаем, тк вс 1, пн - 2 и тд. вычитать безопасно, тк numberOfDay точно не 1 исходя из условия выше
            thisDay = thisDay - 1;

            Node day = daysOfWeek.item((thisDay * 2) - 1);  //<day>
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
                    tableString = tableString + "\n_" + number.getTextContent() + " пара_" +
                            "   `" + periodTimes.item(3).getTextContent() + "-" + periodTimes.item(5).getTextContent()+"`";
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
            if(needLog) log.warning("null ptr in getTodaysTable");
            System.exit(1);
        }
        return tableString;
    }


    public static void completeTask(String chatId, Calendar day){
        //test
        //day.add(Calendar.DATE, 3);
        Integer numberOfWeek;
        Integer numberOfDay = day.get(Calendar.DAY_OF_WEEK);
        if(numberOfDay == Calendar.SUNDAY)
        {
            numberOfWeek = (day.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 2 : 1;
        }else {
            numberOfWeek = (day.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 1 : 2;
        }

        Integer numberOfNextDay = numberOfDay + 1;
        //sendMsg(chatId, getTodayTable(numberOfNextDay, numberOfWeek));

    }


    // То, что выполняется при получении сообщения
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            String answer = new String();

            Calendar now = Calendar.getInstance();
            //test
            //now.add(Calendar.DATE, 3);
            Integer numberOfWeek;
            numberOfWeek = (now.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 1 : 2;
            Integer day = now.get(Calendar.DAY_OF_WEEK);

            switch (message.getText()){
                case "/start":
                    telegramTimer timerTask = new telegramTimer();
                    timerTask.setChatId(chatId);
                    Timer timer = new Timer(true);
                    // будем запускать каждых 10 секунд (10 * 1000 миллисекунд)
                    timer.scheduleAtFixedRate(timerTask, 0, 10*1000);

                    sendMsg(chatId, "sendMeTheTimetable бот приветствует. Вот список того, что я могу:" +
                            "\n/time - расписание звонков" +
                            "\n/today - расписание пар на сегодня" +
                            "\n/week - расписание на эту неделю" +
                            "\n/full - полное расписание на обе недели" +
                            "\n/help - список доступных команд");
                    break;
                case "/help":
                    sendMsg(chatId, "У меня можно узнать расписание.\n/time - расписание звонков" +
                            "\n/today - расписание пар на сегодня" +
                            "\n/week - расписание на эту неделю" +
                            "\n/full - полное расписание на обе недели");
                    break;
                case "/time":
                    sendImageFromUrl(chatId);
                    break;
                case "/today":
                    answer = getTodayTable(day, numberOfWeek);
                    sendMsg(chatId,answer);
                    break;
                case "/week":
                    answer = getWeekTable(numberOfWeek);
                    sendMsg(chatId,answer);
                    break;
                case "/full":
                    answer = getWeekTable(1);
                    sendMsg(chatId,answer);
                    answer = getWeekTable(2);
                    sendMsg(chatId,answer);
                    break;
                default:
                    sendMsg(chatId, "Я не знаю что ответить на это");
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
            sendMessage(s);
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


    private static String getName(Integer numberOfDay){
        switch(numberOfDay){
            case 1: return "Понедельник";
            case 2: return "Вторник";
            case 3: return "Среда";
            case 4: return "Четверг";
            case 5: return "Пятница";
            default: return "";
        }

    }
}


