package com.company;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.text.StringEscapeUtils.unescapeJava;

public class Main {

    private static class Conversation {
        HashMap<String, Integer> map = new HashMap<>();
        String name;
        String filename;
        int allMessagesCount;
    }

    private static class Person {
        String name;
        int messages;
    }

    private static String path = "";

    private static final List<Conversation> allConversations = new ArrayList<>();

    public static void main(String[] args) {
        createAndViewTopList();
        int choice;
        do{
            choice = -1;
            System.out.println("\nChoose what do you want to do: \n1. View Top List again \n2. Check activity in a specific time period \n3. Check specific conversation details \n0. Exit");
            try{
                choice = Integer.parseInt(scan());
            }catch (NumberFormatException e){
                System.out.println("\nChoose a correct option.");
            }

            if(choice == 1) viewTopList();
            if(choice == 2) checkByDate();
            if(choice == 3){
                int ID;
                do{
                    ID = -1;
                    System.out.print("\nChoose ID to check details or 0 to exit: ");
                    try{
                        ID = Integer.parseInt(scan());
                    }catch(NumberFormatException e){
                        System.out.println("\nChoose a correct option.");
                    }
                    if(ID>0){
                        viewDetails(ID);
                    }
                }while(ID != 0);
            }
        }while(choice != 0);
    }

    private static void createAndViewTopList() {
        boolean error = true;
        do {
            System.out.println("\\inbox folder path:");
            path = scan();
            try {
                createTopList(path);
                error = false;
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }
        } while (error);
        sortConversations();
        viewTopList();
    }

    private static void createTopList(String path) throws FileNotFoundException {
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
                pair.filename = conversation.getName();
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

    private static JSONObject getMessagesJSONObject(String filename) {
        JSONTokener jt = null;
        try {
            jt = new JSONTokener(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new JSONObject(jt);
    }

    private static String getConversationName(String filename) {
        JSONObject jObj = getMessagesJSONObject(filename);
        return jObj.getString("title");
    }

    private static HashMap<String, Integer> countMessages(String filename) {
        HashMap<String, Integer> messagesCount = new HashMap<>();

        JSONObject jObj = getMessagesJSONObject(filename);
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

    private static void viewTopList() {
        int id = allConversations.size();
        for (Conversation conv : allConversations) {
            System.out.printf("%5d. %8d - %s\n", id--, conv.allMessagesCount, conv.name);
        }
    }

    private static void checkByDate() {

    }

    private static void viewDetails(int ID) {
        if (ID>allConversations.size() || ID <= 0) throw new IllegalArgumentException("Illegal ID.");
        int position = allConversations.size()-ID;
        System.out.printf("\nShowing details for %s:\n", allConversations.get(position).name);

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


        File conversation = new File(path+"\\"+allConversations.get(position).filename);
        File[] files = conversation.listFiles();
        long earliest = 0;
        long latest = 0;

        //Enters each file and checks the date of first and last message
        for (File file : files) {
            if (file.getName().matches(".*(\\.json)")) {
                JSONObject jObj = getMessagesJSONObject(file.getAbsolutePath());
                JSONArray messages = jObj.getJSONArray("messages");

                long timestamp = ((JSONObject) messages.get(messages.length()-1)).getLong("timestamp_ms");
                if(earliest==0) earliest = timestamp;
                else if(timestamp < earliest) earliest = timestamp;
                timestamp = ((JSONObject) messages.get(0)).getLong("timestamp_ms");
                if(latest==0) latest = timestamp;
                else if(timestamp > latest) latest = timestamp;

            }
        }

        System.out.println("First message:");
        System.out.println(dateFromMillis(earliest));
        System.out.println("Last message:");
        System.out.println(dateFromMillis(latest));

        int choice;
        do{
            choice = -1;
            System.out.println("\nDo you want to see date statistics?\n1. Show me statistics by last months\n2. Show me statistics by last weeks\n3. Show me statistics by last days\n4. Show me statistics from specific date/period\n0. Exit");
            try{
                choice = Integer.parseInt(scan());
            }catch(NumberFormatException e){
                System.out.println("\nChoose a correct option.");
            }
        }while(choice != 0);
    }

    private static String dateFromMillis(long millis) {
        Date date = new Date(millis);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy, HH:mm:ss", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }

    private static String scan() {
        return new Scanner(System.in).nextLine();
    }
}