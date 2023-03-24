/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.osfans.trime.ime.keyboard;

import android.text.TextUtils;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.RimeKeyMapping;
import com.osfans.trime.data.AppPrefs;
import com.osfans.trime.ime.enums.Keycode;
import com.osfans.trime.util.CollectionUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import timber.log.Timber;

/** {@link Key 按鍵}的各種事件（單擊、長按、滑動等） */
public class Event {
  // private String TAG = "Event";
  private final Keyboard mKeyboard;
  private int code;
  private int mask = 0;
  private String text;
  private String label, shiftLabel;
  private String preview;
  private List<String> states;
  private String command;
  private String option;
  private String select;
  private String toggle;
  private String commit;

  private String shiftLock;
  private boolean functional;
  private boolean repeatable;
  private boolean sticky;

  // {send|key}
  private static final Pattern sendPattern = Pattern.compile("\\{[^\\{\\}]+\\}");
  private static final Pattern labelPattern = Pattern.compile("\\{[^\\{\\}]+?\\}");

  public Event(Keyboard keyboard, @NonNull String s) {
    mKeyboard = keyboard;
    if (sendPattern.matcher(s).matches()) {
      label = s.substring(1, s.length() - 1);
      int[] sends = Keycode.parseSend(label); // send
      code = sends[0];
      mask = sends[1];
      if (code > 0 || mask > 0) return;
      if (parseAction(label)) return;
      s = label; // key
      label = null;
    }
    if (Key.presetKeys.containsKey(s)) {
      // todo 把presetKeys缓存为presetKeyEvents，减少重新载入
      Map<String, Object> presetKey = Key.presetKeys.get(s);
      command = CollectionUtils.obtainString(presetKey, "command", "");
      option = CollectionUtils.obtainString(presetKey, "option", "");
      select = CollectionUtils.obtainString(presetKey, "select", "");
      toggle = CollectionUtils.obtainString(presetKey, "toggle", "");
      label = CollectionUtils.obtainString(presetKey, "label", "");
      preview = CollectionUtils.obtainString(presetKey, "preview", "");
      shiftLock = CollectionUtils.obtainString(presetKey, "shift_lock", "");
      commit = CollectionUtils.obtainString(presetKey, "commit", "");
      String send = CollectionUtils.obtainString(presetKey, "send", "");
      if (TextUtils.isEmpty(send) && !TextUtils.isEmpty(command))
        send = "function"; // command默認發function
      int[] sends = Keycode.parseSend(send);
      code = sends[0];
      mask = sends[1];
      parseLabel();
      text = (String) presetKey.get("text");
      if (code < 0 && TextUtils.isEmpty(text)) text = s;
      if (presetKey.containsKey("states")) states = (List<String>) presetKey.get("states");
      sticky = CollectionUtils.obtainBoolean(presetKey, "sticky", false);
      repeatable = CollectionUtils.obtainBoolean(presetKey, "repeatable", false);
      functional = CollectionUtils.obtainBoolean(presetKey, "functional", true);
    } else if ((code = getClickCode(s)) >= 0) {
      parseLabel();
    } else {
      code = 0;
      text = s;
      label = labelPattern.matcher(s).replaceAll("");
    }

    shiftLabel = label;
    if (Keycode.Companion.isStdKey(code)) { // Android keycode区域
      if (Key.getKcm().isPrintingKey(code)) {
        int mMask = KeyEvent.META_SHIFT_ON | mask;
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0, mMask);
        int k = event.getUnicodeChar(mMask);
        Timber.d(
            "shiftLabel = " + shiftLabel + " keycode=" + code + ", mask=" + mMask + ", k=" + k);
        if (k > 0) {
          shiftLabel = "" + ((char) (k));
        }
      }
    }
  }

  public Event(String s) {
    this(null, s);
  }

  public int getCode() {
    return code;
  }

  public int getMask() {
    return mask;
  }

  public String getCommand() {
    return command;
  }

  public String getOption() {
    return option;
  }

  public String getSelect() {
    return select;
  }

  public boolean isFunctional() {
    return functional;
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  public boolean isSticky() {
    return sticky;
  }

  public String getShiftLock() {
    return shiftLock;
  }

  // 快速把字符串解析为event, 暂时只处理了comment类型 不能完全正确处理=，
  private boolean parseAction(String s) {
    boolean result = false;
    String[] strs = s.split(",");
    for (String str : strs) {
      String[] set = str.split("=", 2);
      if (set.length != 2) continue;
      if (set[0].equals("commit")) {
        commit = set[1];
        result = true;
      } else if (set[0].equals("label")) {
        label = set[1];
        result = true;
      } else if (set[0].equals("text")) {
        text = set[1];
        result = true;
      }
    }
    Timber.d("<Event> text=" + text + ", commit=" + commit + ", label=" + label + ", s=" + s);
    return result;
  }

  // TODO 进一步解耦，在Event中去除mKeyboard
  @NonNull
  private String adjustCase(String s) {
    if (TextUtils.isEmpty(s)) return "";
    if (s.length() == 1 && mKeyboard != null && mKeyboard.needUpCase())
      s = s.toUpperCase(Locale.getDefault());
    else if (s.length() == 1
        && mKeyboard != null
        && !Rime.isAsciiMode()
        && mKeyboard.isLabelUppercase()) s = s.toUpperCase(Locale.getDefault());
    return s;
  }

  public String getLabel() {
    if (!TextUtils.isEmpty(toggle)) return (String) states.get(Rime.getOption(toggle) ? 1 : 0);

    if (mKeyboard.isOnlyShiftOn()) {
      if (code >= KeyEvent.KEYCODE_0
          && code <= KeyEvent.KEYCODE_9
          && !AppPrefs.defaultInstance().getKeyboard().getHookShiftNum())
        return adjustCase(shiftLabel);
      if (code >= KeyEvent.KEYCODE_GRAVE && code <= KeyEvent.KEYCODE_SLASH
          || code == KeyEvent.KEYCODE_COMMA
          || code == KeyEvent.KEYCODE_PERIOD) {
        if (!AppPrefs.defaultInstance().getKeyboard().getHookShiftSymbol())
          return adjustCase(shiftLabel);
      }
    } else if (((mKeyboard.getModifer() | mask) & KeyEvent.META_SHIFT_ON) != 0) {
      return adjustCase(shiftLabel);
    }

    return adjustCase(label);
  }

  public String getText() {
    String s = "";
    if (!TextUtils.isEmpty(text)) s = text;
    else if (mKeyboard != null
        && mKeyboard.needUpCase()
        && mask == 0
        && code >= KeyEvent.KEYCODE_A
        && code <= KeyEvent.KEYCODE_Z) s = label;
    return adjustCase(s);
  }

  public String getPreviewText() {
    if (!TextUtils.isEmpty(preview)) return preview;
    return getLabel();
  }

  public String getToggle() {
    if (!TextUtils.isEmpty(toggle)) return toggle;
    return "ascii_mode";
  }

  public String getCommit() {
    return commit;
  }

  private void parseLabel() {
    if (!TextUtils.isEmpty(label)) return;
    int c = code;
    if (c == KeyEvent.KEYCODE_SPACE) {
      label = Rime.getSchemaName();
    } else {
      if (c > 0) label = Keycode.Companion.getDisplayLabel(c, mask);
    }
  }

  public static int getClickCode(String s) {
    int keyCode = -1;
    if (TextUtils.isEmpty(s)) { // 空鍵
      keyCode = 0;
    } else if (Keycode.fromString(s) != Keycode.VoidSymbol) {
      keyCode = Keycode.keyCodeOf(s);
    }
    return keyCode;
  }

  public static boolean hasModifier(int mask, int modifier) {
    return (mask & modifier) > 0;
  }

  // KeyboardEvent 从软键盘的按键keycode（可能含有mask）和mask，分离出rimekeycode和mask构成的数组
  public static int[] getRimeEvent(int code, int mask) {
    int i = RimeKeyMapping.keyCodeToVal(code);
    int m = 0;
    if (hasModifier(mask, KeyEvent.META_SHIFT_ON)) m |= Rime.META_SHIFT_ON;
    if (hasModifier(mask, KeyEvent.META_CTRL_ON)) m |= Rime.META_CTRL_ON;
    if (hasModifier(mask, KeyEvent.META_ALT_ON)) m |= Rime.META_ALT_ON;
    if (hasModifier(mask, KeyEvent.META_SYM_ON)) m |= Rime.META_SYM_ON;
    if (hasModifier(mask, KeyEvent.META_META_ON)) m |= Rime.META_META_ON;
    if (mask == Rime.META_RELEASE_ON) m |= Rime.META_RELEASE_ON;
    Timber.d(
        "<Event> getRimeEvent()\tcode=%d, mask=%d, name=%s\toutput key=%d, meta=%d",
        code, mask, Keycode.keyNameOf(code), i, m);
    return new int[] {i, m};
  }

  public boolean isMeta() {
    int c = getCode();
    return (c == KeyEvent.KEYCODE_META_LEFT || c == KeyEvent.KEYCODE_META_RIGHT);
  }

  public boolean isAlt() {
    int c = getCode();
    return (c == KeyEvent.KEYCODE_ALT_LEFT || c == KeyEvent.KEYCODE_ALT_RIGHT);
  }

  private static final Map<String, Integer> masks =
      new HashMap<String, Integer>() {
        {
          put("Shift", KeyEvent.META_SHIFT_ON);
          put("Control", KeyEvent.META_CTRL_ON);
          put("Alt", KeyEvent.META_ALT_ON);
          put("Meta", KeyEvent.META_META_ON);
          put("SYM", KeyEvent.META_SYM_ON);
        }
      };
}
