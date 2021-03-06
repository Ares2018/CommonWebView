package port;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 序列化map，用于记录图片是否被浏览过
 * Created by wanglinjie.
 * create time:2017/11/9  下午3:48
 */
public class SerializableHashMap implements Serializable {

    private static final long serialVersionUID = 5522846782158681381L;
    private HashMap<Integer, Boolean> map;

    public HashMap<Integer, Boolean> getMap() {
        if (map == null) {
            map = new HashMap<>();
        }
        return map;
    }

    public void setMap(HashMap<Integer, Boolean> map) {
        this.map = map;
    }
}  