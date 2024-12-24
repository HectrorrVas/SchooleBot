package com.codegym.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class SchooleBotApp extends SimpleTelegramBot {

    public static final String TELEGRAM_BOT_TOKEN = "7938288540:AAFmqkro_MVly74qcpvl6ZU3BgIa5R3LLLg";
    public static final String OPEN_AI_TOKEN = "gpt:9ZT5tFmuknnv69DyaBG4JFkblB3TnQwI4ewQzWKutY0lfsgf";

    private final ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode mode = DialogMode.MAIN;
    private String currentScientist = null; // Mantener el contexto del científico seleccionado
    private final ArrayList<String> userMessages = new ArrayList<>();
    private ArrayList<String> list = new ArrayList<>();

    public SchooleBotApp() {
        super(TELEGRAM_BOT_TOKEN);
    }

    // Comando para iniciar el bot
    public void startCommand() {
        mode = DialogMode.MAIN;
        sendPhotoMessage("avatar_main");
        sendTextMessage("¡Bienvenido al bot educativo!");
        sendTextMessage(
                        "/gpt - Pregúntame sobre cualquier tema.\n\n" +
                        "/cientificos - Habla con las mentes más brillantes de la ciencia.\n\n" +
                        "/curiosidades - Datos curiosos y citas históricas.\n\n"
        );

    }

    // Comando para ChatGPT
    public void gptCommand() {
        mode = DialogMode.GPT;
        sendPhotoMessage("gpt");
        sendTextMessage("Estás en el modo GPT. Pregúntame sobre cualquier tema.");
    }

    // Comando para manejo de científicos
    public void dateCommand() {
        mode = DialogMode.DATE;
        currentScientist = null; // Restablecer el contexto de científico seleccionado
        String text = loadMessage("date");
        sendPhotoMessage("date");
       // sendTextMessage(text);
        sendTextButtonsMessage(
                "Selecciona un científico:",
                "date_newton", "Isaac Newton",
                "date_einstein", "Albert Einstein",
                "date_curie", "Marie Curie",
                "date_darwin", "Charles Darwin",
                "date_galilei", "Galileo Galilei",
                "date_aristoteles", "Aristóteles",
                "date_tesla", "Nikola Tesla"
        );
    }

    // Manejo de botones para científicos
    public void dateButton() {
        String key = getButtonKey();
        if (key == null || key.isEmpty()) {
            sendTextMessage("⚠️ No se seleccionó un científico. Inténtalo nuevamente.");
            return;
        }
        currentScientist = key; // Guardar el científico seleccionado
        mode = DialogMode.DATE; // Cambiar al modo de diálogo con científicos
        sendPhotoMessage(key);

        String prompt = loadPrompt(key);
        chatGPT.setPrompt(prompt);

        // Respuesta inicial sin saludo
        String response = chatGPT.sendMessage(prompt, "");
        sendTextMessage(response);
    }

    // Continuar el diálogo con el científico seleccionado
    public void dateDialog() {
        if (currentScientist == null) {
            sendTextMessage("⚠️ No has seleccionado un científico. Usa /cientificos para elegir uno.");
            return;
        }

        var myMessage = sendTextMessage("Escribiendo.. .");

        String text = getMessageText();
        String answer = chatGPT.addMessage(text);
        //sendTextMessage(answer);
        updateTextMessage(myMessage, answer);
    }

    public void curiosidadesComan(){
        mode = DialogMode.MESSAGE;
        String text = loadMessage("message");
        sendPhotoMessage("sabiasque");

        sendTextButtonsMessage(text,
                "message_next", "Matematicas y tecnología",
                "message_date", "Español e ingles"

        );

    }

    public void curiosidadesButton(){
        String key = getButtonKey();
        String promp = loadPrompt(key);
        String history = String.join("\n\n",list);

        var myMessage = sendTextMessage("Dato curioso.. .");
        String answer = chatGPT.sendMessage(promp, history);
        updateTextMessage(myMessage, answer);

        list.clear();
    }

    public void curiosidadesDialog(){
        String text = getMessageText();
        list.add(text);
    }

    // Diálogo general con ChatGPT
    public void gptDialog() {
        if (mode == DialogMode.GPT) {
            String userInput = getMessageText();
            userMessages.add(userInput);

            String prompt = loadPrompt("gpt");
            var message = sendTextMessage("Chat GPT está pensando...");

            String answer = chatGPT.sendMessage(prompt, String.join("\n", userMessages));
            updateTextMessage(message, answer);
        } else {
            sendTextMessage("No estás en modo GPT. Usa /gpt para activarlo.");
        }
    }

    // Mensaje de saludo general
    public void hello() {

        if (mode == DialogMode.GPT) {
            gptDialog();
        } else if (mode == DialogMode.DATE) {
            dateDialog();
        } else if (mode == DialogMode.MESSAGE) {
            curiosidadesDialog();
        } else {
            sendTextMessage("¡Hola! ¿Qué quieres aprender hoy?");
            sendPhotoMessage("cole");
            sendTextMessage("Presiona el comando /start para iniciar");
        }
    }

    // Manejo de botones generales
    public void helloButton() {
        String key = getButtonKey();
        if ("start".equals(key)) {
            startCommand();
        } else if ("stop".equals(key)) {
            sendTextMessage("Gracias por usar el bot. ¡Vuelve pronto!");
        } else {
            sendTextMessage("Opción no reconocida.");
        }
    }

    @Override
    public void onInitialize() {
        addCommandHandler("start", this::startCommand);
        addCommandHandler("gpt", this::gptCommand);
        addCommandHandler("cientificos", this::dateCommand);
      //  addCommandHandler("start", this::helloButton);
        addCommandHandler("curiosidades", this::curiosidadesComan);


        addMessageHandler(this::hello);
        addButtonHandler("^date_.*", this::dateButton); // Para manejar los botones relacionados con científicos
        addButtonHandler("^message_.*", this::curiosidadesButton);

    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new SchooleBotApp());
        } catch (TelegramApiException e) {
            System.err.println("Error al iniciar el bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
