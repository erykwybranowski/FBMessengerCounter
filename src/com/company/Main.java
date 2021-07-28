package com.company;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.commons.text.StringEscapeUtils.unescapeJava;

public class Main {

    private static class Conversation {
        HashMap<String, Integer> map = new HashMap<>();
        String name;
        int allMessagesCount;
    }

    private static class Person {
        String name;
        int messages;
    }

    private static final List<Conversation> allConversations = new ArrayList<>();

    public static void main(String[] args) {
        boolean error = true;
        do {
            try {
                createTopList();
                error = false;
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }
        } while (error);
        sortConversations();
        int id = allConversations.size();
        for (Conversation conv : allConversations) {
            System.out.printf("%5d. %8d - %s\n", id--, conv.allMessagesCount, conv.name);
        }
        int ID;
        while(true){
            try {
                System.out.print("\nChoose ID to check details: ");
                ID = Integer.parseInt(scan());
                viewDetails(ID);
            } catch (NumberFormatException e){
                System.out.print("Illegal ID.\n");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void createTopList() throws FileNotFoundException {
        System.out.println("\\inbox folder path:");
        String path = scan();
        File inbox = new File(path);
        File[] conversations = inbox.listFiles();
        if (conversations == null || conversations.length==0) throw new FileNotFoundException("No conversation folders.");
        int count = 0;
        for (File conversation : conversations) {
            System.out.printf("%d/%d\r", ++count, conversations.length);
            File[] files = conversation.listFiles();
            if (files != null && files.length!=0) {
                Conversation pair = new Conversation();
                String name = getConversationName(conversation.getAbsolutePath()+"\\message_1.json");
                pair.name = repairString(name);
                HashMap<String, Integer> messagesCount = new HashMap<>();
                for (File file : files) {
                    if (file.getName().matches(".*(\\.json)")) {
                        countMessages(file.getAbsolutePath()).forEach((key, value) -> messagesCount.merge(key, value, Integer::sum));
                    }
                }
                int counter = 0;
                for (Map.Entry<String, Integer> entry : messagesCount.entrySet()) {
                    counter += entry.getValue();
                }
                pair.map = messagesCount;
                pair.allMessagesCount = counter;
                allConversations.add(pair);
            }
        }
    }

    private static String getConversationName(String filename) {
        JSONTokener jt = null;
        try {
            jt = new JSONTokener(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JSONObject jObj = new JSONObject(jt);

        return jObj.getString("title");
    }


    private static HashMap<String, Integer> countMessages(String filename) {
        HashMap<String, Integer> messagesCount = new HashMap<>();

        JSONTokener jt = null;
        try {
            jt = new JSONTokener(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        JSONObject jObj = new JSONObject(jt);
        JSONArray messages = jObj.getJSONArray("messages");
        for(Object message : messages){
            String sender_name = ((JSONObject) message).getString("sender_name");
            sender_name = repairString(sender_name);
            int messagesValue = messagesCount.getOrDefault(sender_name, 0);
            messagesCount.put(sender_name, ++messagesValue);
        }
        return messagesCount;
    }

    public static String repairString(String name) {
        name = name.concat("x");
        name = unescapeJava(name);

        ByteBuffer s = StandardCharsets.ISO_8859_1.encode(name);
        CharBuffer t = StandardCharsets.UTF_8.decode(s);
        char[] c = t.array();

        int i = 0;
        while(c[c.length-1-i] != 'x'){
            i++;
        }

        char[] d = new char[c.length-i-1];
        System.arraycopy(c,0,d,0,d.length);
        c = d;

        return new String(c);
    }

    private static void sortConversations() {
        allConversations.sort(Comparator.comparing(o -> o.allMessagesCount));
    }

    private static void sortPersons(List<Person> persons) {
        persons.sort(Comparator.comparing(o -> o.messages));
    }

    private static void viewDetails(int ID) {
        if (ID>allConversations.size() || ID <= 0) throw new IllegalArgumentException("Illegal ID.");
        int position = allConversations.size()-ID;
        System.out.printf("\nShowing details for %s\n", allConversations.get(position).name);

        List<Person> persons = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : allConversations.get(position).map.entrySet()) {
            Person person = new Person();
            person.name = entry.getKey();
            person.messages = entry.getValue();
            persons.add(person);
        }
        sortPersons(persons);
        for (Person person : persons) {
            System.out.printf("%8d - %s\n", person.messages, person.name);
        }
        System.out.printf("%8d - %s\n", allConversations.get(position).allMessagesCount, "All messages");
    }

    private static String scan() {
        return new Scanner(System.in).nextLine();
    }
}