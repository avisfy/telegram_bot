import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Logger;

public class TelegramBot extends TelegramLongPollingBot {

    private static Logger log = Logger.getLogger(TelegramBot.class.getName());
    private static Boolean needLog = true;
    private static String username = "";
    private static String token = "";
    private static Document time;
    private static Document table;

    private static void initTimetables()
    {
        try{
            // Создается построитель документа
            DocumentBuilder documentBuilderTime = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            //DocumentBuilder documentBuilderTable = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            // Создается дерево DOM документа из файла
            time = documentBuilderTime.parse(new File("rt.xml"));
            //table = documentBuilderTable.parse(new File("sub.xml"));
            if (needLog) log.info("initTables");
            //for (Integer i = 0; i< 6; i++)
                //log.info(time.getElementsByTagName("begin").item(i).getTextContent());
        }catch(FileNotFoundException e){
            if (needLog) log.warning("file not found");
            System.exit(1);
        }catch(NullPointerException e) {
            if(needLog) log.warning("null ptr");
            System.exit(1);
        }catch (ParserConfigurationException e) {
            e.printStackTrace(System.out);
        }catch (SAXException e) {
            e.printStackTrace();
        }catch (IOException e) {
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
        if(needLog) log.info("main");
    }

    private void initConnectionData(){
        try{
            if (username.isEmpty() || token.isEmpty()) {
                // Создается построитель документа
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                // Создается дерево DOM документа из файла
                Document document = documentBuilder.parse(new File("impData.xml"));
                if (needLog) log.info("getUs2");

                username = document.getElementsByTagName("username").item(0).getTextContent();
                token = document.getElementsByTagName("token").item(0).getTextContent();
                if (needLog) log.info("init username:" + username + " token:" + token);
            }
        }catch(FileNotFoundException e){
            if (needLog) log.warning("file not found");
            System.exit(1);
        }catch(NullPointerException e) {
            if(needLog) log.warning("null ptr");
            System.exit(1);
        }catch (ParserConfigurationException e) {
            e.printStackTrace(System.out);
        }catch (SAXException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        if(needLog) log.info("getUs");
        initConnectionData();
        return username;
    }

    @Override
    public String getBotToken() {
        if(needLog) log.info("getTok");
        initConnectionData();
        return token;
    }

    // То, что выполняется при получении сообщения
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            Calendar c = Calendar.getInstance();
            Integer numberOfWeek;
            if ((Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2) == 0){
                numberOfWeek = 1;
            }
            else {
                numberOfWeek = 2;
            }
            switch (message.getText()){
                case "/help":
                    sendMsg(chatId, "У меня можно узнать расписание.\n/time - расписание звонков" +
                            "\n/today - расписание пар на сегодня" +
                            "\n/now - инфо о ближайшей паре" +
                            "\n/week - расписание на эту неделю" +
                            "\n/full - полное расписание на обе недели" +
                            "\n/help - список доступных команд");
                case "/start":
                    sendMsg(chatId, "sendMeTheTimetable бот приветствует. Вот список того, что я могу:" +
                            "\n/time - расписание звонков" +
                            "\n/today - расписание пар на сегодня" +
                            "\n/now - инфо о ближайшей паре" +
                            "\n/week - расписание на эту неделю" +
                            "\n/full - полное расписание на обе недели");
                    break;
                case "/time":
                    sendImageFromUrl(chatId);
                    break;
                case "/today":
                    Integer dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                    sendMsg(chatId,numberOfWeek.toString());
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
