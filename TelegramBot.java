import com.sun.deploy.security.ValidationState;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class TelegramBot extends TelegramLongPollingBot {
    private static Logger log = Logger.getLogger(TelegramBot.class.getName());
    private static Boolean needLog = true;
    private static TelegramTimer timerTask;

    private static String username = "";
    private static String token = "";

    private static NodeList childrenTime;
    private static NodeList childrenTable;
    public enum TYPE_TABLE{SMALL, NORMAL};
    private static int BEFORE_MIN = 15;


    public static void main(String[] args) {
        //log settings
        log.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        log.addHandler(handler);

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
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


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
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        //if (needLog) log.info("getUs");
        initConnectionData();
        return username;
    }


    @Override
    public String getBotToken() {
        //if (needLog) log.info("getTok");
        initConnectionData();
        return token;
    }


    // То, что выполняется при получении сообщения
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            String answer;

            Calendar now = Calendar.getInstance();
            now.setFirstDayOfWeek(Calendar.MONDAY);
            //test
            //now.add(Calendar.DATE, 3);
            Integer numberOfWeek;
            numberOfWeek = (now.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 1 : 2;
            Integer day = now.get(Calendar.DAY_OF_WEEK);

            switch (message.getText()){
                case "/start":
                    if(timerTask == null) {
                        timerTask = new TelegramTimer(chatId, this);
                        Timer timer = new Timer(true);
                        // будем запускать каждые 60 секунд (60 * 1000 миллисекунд)
                        timer.scheduleAtFixedRate(timerTask, 0, 60*1000);
                        if(needLog) log.info("timer started");
                    }
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
                    answer = getTodayTable(day, numberOfWeek, TYPE_TABLE.NORMAL);
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
                case  "/tomorrow":
                    now.add(Calendar.DAY_OF_MONTH, 1);
                    //int nextDay = now.get(Calendar.DAY_OF_WEEK);
                    //answer = getTodayTable(nextDay, numberOfWeek, TYPE_TABLE.NORMAL);
                    //sendMsg(chatId,answer);
                    completeTaskNextDay(chatId, now, TYPE_TABLE.NORMAL);
                    break;
                default:
                    sendMsg(chatId, "Я не знаю что ответить на это");
            }
        }
    }


    private void showMyKeyboard(SendMessage s) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        KeyboardRow row = new KeyboardRow();
        // Set each button, you can also use KeyboardButton objects if you need something else than text
        row.add("/time");
        row.add("/today");
        row.add("/tomorrow");
        //добавляем первую строку
        keyboard.add(row);
        row = new KeyboardRow();
        row.add("/week");
        row.add("/full");
        row.add("/help");
        //добавляем вторую строку
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        //добавляем к сообщению
        s.setReplyMarkup(keyboardMarkup);
    }


    private void sendMsg(String chatId, String text) {
        SendMessage s = new SendMessage();
        s.enableMarkdown(true);
        s.setChatId(chatId);
        //s.setReplyToMessageId(message.getMessageId());
        s.setText(text);
         showMyKeyboard(s);
        try {
            //sendMessage(s);
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


    private static String getWeekTable(Integer thisWeek) {
        StringBuffer tableString = new StringBuffer("Неделя ");
        tableString.append(thisWeek).append("\n");
        for (int j = 1; j <= 5; j++) {
            if (j != 1) tableString.append("------------------------------------------------------------\n");
            tableString.append("\n*").append(getName(j)).append("*\n").append(getTodayTable(j+1 , thisWeek, TYPE_TABLE.NORMAL));
        }
        return tableString.toString();
    }


    private static String getTodayTable(Integer thisDay, Integer thisWeek, TYPE_TABLE typeTable) {
        StringBuffer tableString = new StringBuffer(380);
        try {
            Node week = childrenTable.item((thisWeek * 2) - 1); //<week>
            NodeList daysOfWeek = week.getChildNodes();
            if ((thisDay == Calendar.SATURDAY) || (thisDay == Calendar.SUNDAY)) {
                tableString.append("Выходной день - пар нет!");
                return tableString.toString();
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
            for (int i = 1; i <= 7; i++) {
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
                    tableString.append("\n_").append(number.getTextContent()).append(" пара_").append("   `");
                    tableString.append(periodTimes.item(3).getTextContent()).append("-");
                    tableString.append(periodTimes.item(5).getTextContent()).append("`");
                }
                //если название пары пусто, значит пары нет вообще, безымянных пар нет
                if (name.getTextContent().equals(" ")) {
                    if(typeTable == TYPE_TABLE.NORMAL) {
                        tableString.append("\n");
                    }
                    continue;
                } else {
                    tableString.append("\n").append(name.getTextContent());
                }
                if (!room.getTextContent().equals(" ")) {
                    tableString.append("\n").append(room.getTextContent());
                }
                else {
                    tableString.append("\n");
                }
                if (!prof.getTextContent().equals(" ")) {
                    tableString.append("\n").append(prof.getTextContent());
                }
                if(typeTable == TYPE_TABLE.NORMAL)
                    tableString.append("\n");
            }
        }catch(NullPointerException e){
            if(needLog) log.warning("null ptr in getTodaysTable");
            System.exit(1);
        }
        return tableString.toString();
    }


    public void completeTaskNextDay(String chatId, Calendar day, TYPE_TABLE typeTable){
        //test
        //day.add(Calendar.DATE, 3);
        int numberOfWeek;
        int numberOfDay = day.get(Calendar.DAY_OF_WEEK);
        if(numberOfDay == Calendar.SUNDAY)
        {
            numberOfWeek = (day.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 2 : 1;
        }else {
            numberOfWeek = (day.get(Calendar.WEEK_OF_YEAR) % 2) == 0 ? 1 : 2;
        }
        day.add(Calendar.DATE, 1);
        int numberOfNextDay = day.get(Calendar.DAY_OF_WEEK);
        sendMsg(chatId, getTodayTable(numberOfNextDay, numberOfWeek, typeTable));

    }


    public void completeTaskBefore(String chatId, int thisNumber, int thisWeek, int thisDay)
    {
        StringBuffer answer = new StringBuffer(50);
        try {
            Node week = childrenTable.item((thisWeek * 2) - 1); //<week>
            NodeList daysOfWeek = week.getChildNodes();
            //вычитаем, тк вс 1, пн - 2 и тд. вычитать безопасно, numberOfDay точно не 1 тк в вс нет пар и рассылка не производится
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
            numberOfSub = (thisNumber * 2) - 1;
            subject = subjectsList.item(numberOfSub);
            subjectInfo = subject.getChildNodes();
            number = subjectInfo.item(1);
            name = subjectInfo.item(3);
            room = subjectInfo.item(5);
            prof = subjectInfo.item(7);

            period = childrenTime.item(numberOfSub);
            periodTimes = period.getChildNodes();

            if (number.getTextContent().equals(" ")) {
                return;
            } else {
                answer.append(number.getTextContent()).append(" пара ").append("   ");
                answer.append(periodTimes.item(3).getTextContent()).append("-");
                answer.append(periodTimes.item(5).getTextContent());
            }
            // еслт пара пустая, ничего не пишем, уведомлять не нужно
            if (name.getTextContent().equals(" ")) {
                return;
            } else {
                answer.append("\n").append(name.getTextContent());
            }
            if (!room.getTextContent().equals(" ")) {
                answer.append("\n").append(room.getTextContent());
            }
            if (!prof.getTextContent().equals(" ")) {
                answer.append("\n").append(prof.getTextContent());
            }
        }catch(NullPointerException e){
            if(needLog) log.warning("null ptr in getTodaysTable");
            System.exit(1);
        }
        sendMsg(chatId, answer.toString());
    }

    //возвращает номер нужной пары, 0 если в ближайшее время вообще нет пар
    public int checkTime(Calendar timeNow)
    {
        timeNow.add(Calendar.MINUTE, BEFORE_MIN);
        try {
            Node period; //<period>
            NodeList periodTimes;
            int numberOfSub;
            int tableHour = 0;
            int tableMinute = 0;
            for (int i = 1; i <= 7; i++) {
                numberOfSub = (i * 2) - 1;

                period = childrenTime.item(numberOfSub);
                periodTimes = period.getChildNodes();
                String pairTime = periodTimes.item(3).getTextContent();
                tableHour = Integer.parseInt(pairTime.substring(0, 2), 10);
                tableMinute = Integer.parseInt(pairTime.substring(3), 10);
                //Calendar pair;
                //pair = (Calendar) timeNow.clone();
                //pair.set(Calendar.HOUR_OF_DAY,  tableHour);
                //pair.set(Calendar.MINUTE,  tableMinute);
                //pair.add(Calendar.MINUTE, -BEFORE_MIN);
                //if(needLog) log.fine(pair.getTime().toString()+" "+timeNow.getTime().toString());
                //if(pair.compareTo(timeNow) == 0)
                if(tableHour == timeNow.get(Calendar.HOUR_OF_DAY) && (timeNow.get(Calendar.MINUTE) == tableMinute))
                    return i;
            }
        }catch(NullPointerException e){
            if(needLog) log.warning("null ptr in checkTime");
            System.exit(1);
        }catch (NumberFormatException e){
            if(needLog) log.warning("number format error");
            System.exit(1);
        }
        return 0;
    }


    private static String getName(int numberOfDay){
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


