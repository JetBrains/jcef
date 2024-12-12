package com.jetbrains.cef.remote.menu;

import com.jetbrains.cef.remote.RemoteServerObject;
import com.jetbrains.cef.remote.RpcExecutor;
import com.jetbrains.cef.remote.thrift_codegen.RObject;
import org.cef.callback.CefMenuModel;
import org.cef.misc.BoolRef;
import org.cef.misc.IntRef;

public class RemoteMenuModel extends RemoteServerObject implements CefMenuModel {
    public RemoteMenuModel(RpcExecutor server, RObject robj) {
        super(server, robj);
    }

    @Override
    protected void disposeOnServerImpl() {
    }

    @Override
    public void flush() {
    }

    @Override
    public boolean clear() {
        return myServer.execObj((s) -> s.clear(thriftId()));
    }

    @Override
    public int getCount() {
        return myServer.execObj((s -> s.get_count(thriftId())));
    }

    @Override
    public boolean addSeparator() {
        return myServer.execObj((s -> s.add_separator(thriftId())));
    }

    @Override
    public boolean addItem(int command_id, String label) {
        return myServer.execObj((s -> s.add_item(thriftId(), command_id, label)));
    }

    @Override
    public boolean addCheckItem(int command_id, String label) {
        return myServer.execObj((s -> s.add_check_item(thriftId(), command_id, label)));
    }

    @Override
    public boolean addRadioItem(int command_id, String label, int group_id) {
        return myServer.execObj((s -> s.add_radio_item(thriftId(), command_id, label, group_id)));
    }

    @Override
    public CefMenuModel addSubMenu(int command_id, String label) {
        RObject rObject = myServer.execObj(s -> s.add_sub_menu(thriftId(), command_id, label));
        return new RemoteMenuModel(myServer, rObject);
    }

    @Override
    public boolean insertSeparatorAt(int index) {
        return myServer.execObj((s -> s.insert_separator_at(thriftId(), index)));
    }

    @Override
    public boolean insertItemAt(int index, int command_id, String label) {
        return myServer.execObj((s -> s.insert_item_at(thriftId(), index, command_id, label)));
    }

    @Override
    public boolean insertCheckItemAt(int index, int command_id, String label) {
        return myServer.execObj((s -> s.insert_check_item_at(thriftId(), index, command_id, label)));
    }

    @Override
    public boolean insertRadioItemAt(int index, int command_id, String label, int group_id) {
        return myServer.execObj((s -> s.insert_radio_item_at(thriftId(), index, command_id, label, group_id)));
    }

    @Override
    public CefMenuModel insertSubMenuAt(int index, int command_id, String label) {
        RObject rObject = myServer.execObj(s -> s.insert_sub_menu_at(thriftId(), index, command_id, label));
        return new RemoteMenuModel(myServer, rObject);
    }

    @Override
    public boolean remove(int command_id) {
        return myServer.execObj((s -> s.remove(thriftId(), command_id)));
    }

    @Override
    public boolean removeAt(int index) {
        return myServer.execObj((s -> s.remove_at(thriftId(), index)));
    }

    @Override
    public int getIndexOf(int command_id) {
        return myServer.execObj((s)-> s.get_index_of(thriftId(), command_id));
    }

    @Override
    public int getCommandIdAt(int index) {
        return myServer.execObj((s) -> s.get_command_id_at(thriftId(), index));
    }

    @Override
    public boolean setCommandIdAt(int index, int command_id) {
        return myServer.execObj((s) -> s.set_command_id_at(thriftId(), index, command_id));
    }

    @Override
    public String getLabel(int command_id) {
        return myServer.execObj((s) -> s.get_label(thriftId(), command_id));
    }

    @Override
    public String getLabelAt(int index) {
        return myServer.execObj((s) -> s.get_label_at(thriftId(), index));
    }

    @Override
    public boolean setLabel(int command_id, String label) {
        return myServer.execObj((s) -> s.set_label(thriftId(), command_id, label));
    }

    @Override
    public boolean setLabelAt(int index, String label) {
        return myServer.execObj((s) -> s.set_label_at(thriftId(), index, label));
    }

    @Override
    public MenuItemType getType(int command_id) {
        int type = myServer.execObj((s) -> s.get_type(thriftId(), command_id));
        return MenuItemType.values()[type];
    }

    @Override
    public MenuItemType getTypeAt(int index) {
        return MenuItemType.values()[myServer.execObj((s) -> s.get_type_at(thriftId(), index))];
    }

    @Override
    public int getGroupId(int command_id) {
        return myServer.execObj((s) -> s.get_group_id(thriftId(), command_id));
    }

    @Override
    public int getGroupIdAt(int index) {
        return myServer.execObj((s) -> s.get_group_id_at(thriftId(), index));
    }

    @Override
    public boolean setGroupId(int command_id, int group_id) {
        return myServer.execObj((s) -> s.set_group_id(thriftId(), command_id, group_id));
    }

    @Override
    public boolean setGroupIdAt(int index, int group_id) {
        return myServer.execObj((s) -> s.set_group_id_at(thriftId(), index, group_id));
    }

    @Override
    public CefMenuModel getSubMenu(int command_id) {
        RObject rObject = myServer.execObj((s) -> s.get_sub_menu(thriftId(), command_id));
        return new RemoteMenuModel(myServer, rObject);
    }

    @Override
    public CefMenuModel getSubMenuAt(int index) {
        RObject rObject = myServer.execObj((s) -> s.get_sub_menu_at(thriftId(), index));
        return new RemoteMenuModel(myServer, rObject);
    }

    @Override
    public boolean isVisible(int command_id) {
        return myServer.execObj((s) -> s.is_visible(thriftId(), command_id));
    }

    @Override
    public boolean isVisibleAt(int index) {
        return myServer.execObj((s) -> s.is_visible_at(thriftId(), index));
    }

    @Override
    public boolean setVisible(int command_id, boolean visible) {
        return myServer.execObj((s) -> s.set_visible(thriftId(), command_id, visible));
    }

    @Override
    public boolean setVisibleAt(int index, boolean visible) {
        return myServer.execObj((s) -> s.set_visible_at(thriftId(), index, visible));
    }

    @Override
    public boolean isEnabled(int command_id) {
        return myServer.execObj((s) -> s.is_enabled(thriftId(), command_id));
    }

    @Override
    public boolean isEnabledAt(int index) {
        return myServer.execObj((s) -> s.is_enabled_at(thriftId(), index));
    }

    @Override
    public boolean setEnabled(int command_id, boolean enabled) {
        return myServer.execObj((s) -> s.set_enabled(thriftId(), command_id, enabled));
    }

    @Override
    public boolean setEnabledAt(int index, boolean enabled) {
        return myServer.execObj((s) -> s.set_enabled_at(thriftId(), index, enabled));
    }

    @Override
    public boolean isChecked(int command_id) {
        return myServer.execObj((s) -> s.is_checked(thriftId(), command_id));
    }

    @Override
    public boolean isCheckedAt(int index) {
        return myServer.execObj((s) -> s.is_checked_at(thriftId(), index));
    }

    @Override
    public boolean setChecked(int command_id, boolean checked) {
        return myServer.execObj((s) -> s.set_checked(thriftId(), command_id, checked));
    }

    @Override
    public boolean setCheckedAt(int index, boolean checked) {
        return myServer.execObj((s) -> s.set_checked_at(thriftId(), index, checked));
    }

    @Override
    public boolean hasAccelerator(int command_id) {
        return false;
    }

    @Override
    public boolean hasAcceleratorAt(int index) {
        return false;
    }

    @Override
    public boolean setAccelerator(int command_id, int key_code, boolean shift_pressed, boolean ctrl_pressed, boolean alt_pressed) {
        return false;
    }

    @Override
    public boolean setAcceleratorAt(int index, int key_code, boolean shift_pressed, boolean ctrl_pressed, boolean alt_pressed) {
        return false;
    }

    @Override
    public boolean removeAccelerator(int command_id) {
        return false;
    }

    @Override
    public boolean removeAcceleratorAt(int index) {
        return false;
    }

    @Override
    public boolean getAccelerator(int command_id, IntRef key_code, BoolRef shift_pressed, BoolRef ctrl_pressed, BoolRef alt_pressed) {
        return false;
    }

    @Override
    public boolean getAcceleratorAt(int index, IntRef key_code, BoolRef shift_pressed, BoolRef ctrl_pressed, BoolRef alt_pressed) {
        return false;
    }

//    @Override
//    public String toString() {
//        StringBuilder builder = new StringBuilder();
//        buildMenuTree(builder, this, 0);
//        return builder.toString();
//    }
//
//    private static void buildMenuTree(StringBuilder builder, CefMenuModel menu, int depth) {
//        int itemCount = menu.getCount();
//        String indent = "  ".repeat(depth);
//
//        for (int i = 0; i < itemCount; i++) {
//            String name = menu.getLabelAt(i) + "(" + menu.getCommandIdAt(i) + ")";
//            MenuItemType type = menu.getTypeAt(i);
//
//            builder.append(indent).append("- ").append(name).append(" (").append(type).append(")").append("\n");
//
//            if (type == MenuItemType.MENUITEMTYPE_SUBMENU) {
//                // If this item is a submenu, recursively get its children
//                CefMenuModel subMenu = menu.getSubMenuAt(i);
//                buildMenuTree(builder, subMenu, depth + 1); // Increase depth for submenu
//            }
//        }
//    }
}
