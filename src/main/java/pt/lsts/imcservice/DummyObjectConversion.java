package pt.lsts.imcservice;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.msg.Message;

import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class DummyObjectConversion {

    public static String asJson(Message msg) {
        return asJsonObject(msg, true).toString();
    }

    private static JsonObject asJsonObject(Message msg, boolean toplevel) {


        JsonObject obj = new JsonObject();
        JsonObject message = new JsonObject();
        if (msg == null)
            return message;

        obj.add("abbrev", msg.getClass().getSimpleName());

        if (toplevel) {
            obj.add("timestamp", ""+msg.timestamp);
            obj.add("src", ""+msg.src);
            obj.add("src_ent", ""+msg.src_ent);
            obj.add("dst", ""+msg.dst);
            obj.add("dst_ent", ""+msg.dst_ent);
        }

        for (Field f : msg.getClass().getDeclaredFields()) {
            FieldType type = f.getAnnotation(FieldType.class);
            if (type == null)
                continue;
            try {
                switch (type.type()) {
                    case TYPE_MESSAGE:
                        obj.add(f.getName(), asJsonObject((Message) f.get(msg), false));
                        break;
                    case TYPE_RAWDATA: {
                        byte[] data = (byte[]) f.get(msg);
                        obj.add(f.getName(), DatatypeConverter.printHexBinary(data));
                        break;
                    }
                    case TYPE_MESSAGELIST: {
                        @SuppressWarnings("unchecked")
                        ArrayList<Message> msgs = (ArrayList<Message>) f.get(msg);
                        JsonArray array = new JsonArray();
                        if (msgs != null)
                            for (Message m : msgs)
                                array.add(asJsonObject(m, false));
                        obj.add(f.getName(), array);
                        break;
                    }
                    default:
                        obj.add(f.getName(), "" + f.get(msg));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        message.add("message", obj);
        return message;
    }
}
