package com.jetbrains.cef.remote.menu;

import com.jetbrains.cef.remote.thrift_codegen.MenuItem;
import org.cef.callback.CefMenuModel;
import org.cef.misc.BoolRef;
import org.cef.misc.IntRef;

import java.util.ArrayList;
import java.util.List;

public class RemoteMenuModel implements CefMenuModel {
    List<MenuItem> myThriftModel;
    public RemoteMenuModel(List<MenuItem> thriftModel) {
        myThriftModel = thriftModel;
    }

    @Override
    public boolean clear() {
        myThriftModel.clear();
        return true;
    }

    @Override
    public int getCount() {
        return myThriftModel.size();
    }

    @Override
    public boolean addSeparator() {
        MenuItem item = new MenuItem();
        item.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_SEPARATOR;
        return append(item);
    }

    @Override
    public boolean addItem(int command_id, String label) {
        MenuItem item = new MenuItem();
        item.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_COMMAND;
        item.command_id = command_id;
        item.label = label;
        return append(item);
    }

    @Override
    public boolean addCheckItem(int command_id, String label) {
        MenuItem item = new MenuItem();
        item.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_CHECK;
        item.command_id = command_id;
        item.label = label;
        return append(item);
    }

    @Override
    public boolean addRadioItem(int command_id, String label, int group_id) {
        MenuItem item = new MenuItem();
        item.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_RADIO;
        item.command_id = command_id;
        item.label = label;
        item.group_id = group_id;
        return append(item);
    }

    @Override
    public CefMenuModel addSubMenu(int command_id, String label) {
        MenuItem item = new MenuItem();
        item.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_SUBMENU;
        item.command_id = command_id;
        item.label = label;
        item.sub_menu = new ArrayList<>();
        myThriftModel.add(item);
        return new RemoteMenuModel(item.sub_menu);
    }

    @Override
    public boolean insertSeparatorAt(int index) {
        MenuItem item = new MenuItem();
        item.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_SEPARATOR;
        return insert(item, index);
    }

    @Override
    public boolean insertItemAt(int index, int command_id, String label) {
        index = Math.min(index, myThriftModel.size());
        MenuItem i = new MenuItem();
        i.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_COMMAND;
        i.command_id = command_id;
        i.label = label;
        return insert(i, index);
    }

    @Override
    public boolean insertCheckItemAt(int index, int command_id, String label) {
        MenuItem i = new MenuItem();
        i.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_CHECK;
        i.command_id = command_id;
        i.label = label;
        return insert(i, index);
    }

    @Override
    public boolean insertRadioItemAt(int index, int command_id, String label, int group_id) {
        MenuItem i = new MenuItem();
        i.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_RADIO;
        i.command_id = command_id;
        i.label = label;
        i.group_id = group_id;
        return insert(i, index);
    }

    @Override
    public CefMenuModel insertSubMenuAt(int index, int command_id, String label) {
        MenuItem i = new MenuItem();
        i.type = com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_SUBMENU;
        i.command_id = command_id;
        i.label = label;
        i.sub_menu = new ArrayList<>();
        myThriftModel.add(index, i);
        return new RemoteMenuModel(i.sub_menu);
    }

    @Override
    public boolean remove(int command_id) {
        return myThriftModel.removeIf(i -> i.command_id == command_id);
    }

    @Override
    public boolean removeAt(int index) {
        try {
            myThriftModel.remove(index);
        } catch (IndexOutOfBoundsException e) {
            return false;
        }

        return true;
    }

    @Override
    public int getIndexOf(int command_id) {
        for (int i = 0; i < myThriftModel.size(); i++) {
            if (myThriftModel.get(i).command_id == command_id)
                return i;
        }

        return -1;
    }

    @Override
    public int getCommandIdAt(int index) {
        if (index < 0 || index >= myThriftModel.size())
            return -1;

        return myThriftModel.get(index).command_id;
    }

    @Override
    public boolean setCommandIdAt(int index, int command_id) {
        if (index < 0 || index >= myThriftModel.size()) {
            return false;
        }
        myThriftModel.get(index).command_id = command_id;
        return true;
    }

    @Override
    public String getLabel(int command_id) {
        int index = getIndexOf(command_id);
        return getLabelAt(index);
    }

    @Override
    public String getLabelAt(int index) {
        MenuItem item = getItem(index);
        if (item == null) {
            return null;
        }

        return myThriftModel.get(index).label;
    }

    @Override
    public boolean setLabel(int command_id, String label) {
        int index = getIndexOf(command_id);
        return setLabelAt(index, label);
    }

    @Override
    public boolean setLabelAt(int index, String label) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        item.label = label;
        return true;
    }

    @Override
    public MenuItemType getType(int command_id) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return null;
        }
        return convert(item.type);
    }

    @Override
    public MenuItemType getTypeAt(int index) {
        MenuItem item = getItem(index);
        if (item == null) {
            return null;
        }
        return convert(item.type);
    }

    @Override
    public int getGroupId(int command_id) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return -1;
        }
        return item.group_id;
    }

    @Override
    public int getGroupIdAt(int index) {
        MenuItem item = getItem(index);
        if (item == null) {
            return -1;
        }
        return item.group_id;
    }

    @Override
    public boolean setGroupId(int command_id, int group_id) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return false;
        }
        item.group_id = group_id;
        return true;
    }

    @Override
    public boolean setGroupIdAt(int index, int group_id) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        item.group_id = group_id;
        return true;
    }

    @Override
    public CefMenuModel getSubMenu(int command_id) {
        int index = getIndexOf(command_id);
        return getSubMenuAt(index);
    }

    @Override
    public CefMenuModel getSubMenuAt(int index) {
        MenuItem item = getItem(index);
        if (item == null) {
            return null;
        }
        return new RemoteMenuModel(item.sub_menu);
    }

    @Override
    public boolean isVisible(int command_id) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return false;
        }
        return item.visible;
    }

    @Override
    public boolean isVisibleAt(int index) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        return item.visible;
    }

    @Override
    public boolean setVisible(int command_id, boolean visible) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return false;
        }
        item.visible = visible;
        return true;
    }

    @Override
    public boolean setVisibleAt(int index, boolean visible) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        item.visible = visible;
        return true;
    }

    @Override
    public boolean isEnabled(int command_id) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return false;
        }
        return item.enabled;
    }

    @Override
    public boolean isEnabledAt(int index) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        return item.enabled;
    }

    @Override
    public boolean setEnabled(int command_id, boolean enabled) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return false;
        }
        item.enabled = enabled;
        return true;
    }

    @Override
    public boolean setEnabledAt(int index, boolean enabled) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        item.enabled = enabled;
        return true;
    }

    @Override
    public boolean isChecked(int command_id) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return false;
        }
        return item.checked;
    }

    @Override
    public boolean isCheckedAt(int index) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        return item.checked;
    }

    @Override
    public boolean setChecked(int command_id, boolean checked) {
        MenuItem item = getItem(getIndexOf(command_id));
        if (item == null) {
            return false;
        }
        item.checked = checked;
        return true;
    }

    @Override
    public boolean setCheckedAt(int index, boolean checked) {
        MenuItem item = getItem(index);
        if (item == null) {
            return false;
        }
        item.checked = checked;
        return true;
    }

    // Not supported yet
    @Override
    public boolean hasAccelerator(int command_id) {
        return false;
    }

    // Not supported
    @Override
    public boolean hasAcceleratorAt(int index) {
        return false;
    }

    // Not supported
    @Override
    public boolean setAccelerator(int command_id, int key_code, boolean shift_pressed, boolean ctrl_pressed, boolean alt_pressed) {
        return false;
    }

    // Not supported
    @Override
    public boolean setAcceleratorAt(int index, int key_code, boolean shift_pressed, boolean ctrl_pressed, boolean alt_pressed) {
        return false;
    }

    // Not supported
    @Override
    public boolean removeAccelerator(int command_id) {
        return false;
    }

    // Not supported
    @Override
    public boolean removeAcceleratorAt(int index) {
        return false;
    }

    // Not supported
    @Override
    public boolean getAccelerator(int command_id, IntRef key_code, BoolRef shift_pressed, BoolRef ctrl_pressed, BoolRef alt_pressed) {
        return false;
    }

    // Not supported
    @Override
    public boolean getAcceleratorAt(int index, IntRef key_code, BoolRef shift_pressed, BoolRef ctrl_pressed, BoolRef alt_pressed) {
        return false;
    }

    private boolean append(MenuItem item) {
        if (!validate(item)) {
            return false;
        }

        myThriftModel.add(item);
        return true;
    }

    private boolean insert(MenuItem item, int index) {
        if (!validate(item) || index < 0 || index > myThriftModel.size()) {
            return false;
        }

        myThriftModel.add(index, item);
        return true;
    }

    private MenuItem getItem(int index) {
        if (index < 0 || index >= myThriftModel.size()) {
            return null;
        }

        return myThriftModel.get(index);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean validate(MenuItem item) {
        if (item.type == com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_NONE) {
            return false;
        }
        if (item.type == com.jetbrains.cef.remote.thrift_codegen.MenuItemType.MENUITEMTYPE_SEPARATOR) {
            return item.command_id == -1;
        }

        return item.command_id >= 0;
    }

    public static CefMenuModel.MenuItemType convert(com.jetbrains.cef.remote.thrift_codegen.MenuItemType thriftType) {
        if (thriftType == null) {
            return null;
        }

        return switch (thriftType) {
            case MENUITEMTYPE_NONE -> MenuItemType.MENUITEMTYPE_NONE;
            case MENUITEMTYPE_COMMAND -> MenuItemType.MENUITEMTYPE_COMMAND;
            case MENUITEMTYPE_CHECK -> MenuItemType.MENUITEMTYPE_CHECK;
            case MENUITEMTYPE_RADIO -> MenuItemType.MENUITEMTYPE_RADIO;
            case MENUITEMTYPE_SEPARATOR -> MenuItemType.MENUITEMTYPE_SEPARATOR;
            case MENUITEMTYPE_SUBMENU -> MenuItemType.MENUITEMTYPE_SUBMENU;
            default -> null;
        };
    }

    public List<MenuItem> getThriftModel() {
        return myThriftModel;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildMenuTree(builder, this, 0);
        return builder.toString();
    }

    private static void buildMenuTree(StringBuilder builder, CefMenuModel menu, int depth) {
        int itemCount = menu.getCount();
        String indent = "  ".repeat(depth);

        for (int i = 0; i < itemCount; i++) {
            String name = menu.getLabelAt(i) + "(" + menu.getCommandIdAt(i) + ")";
            MenuItemType type = menu.getTypeAt(i);

            builder.append(indent).append("- ").append(name).append(" (").append(type).append(")").append("\n");

            if (type == MenuItemType.MENUITEMTYPE_SUBMENU) {
                CefMenuModel subMenu = menu.getSubMenuAt(i);
                buildMenuTree(builder, subMenu, depth + 1);
            }
        }
    }
}
