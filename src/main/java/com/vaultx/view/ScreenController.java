package com.vaultx.view;

import javafx.scene.Node;

public interface ScreenController {
    Node view();
    default void dispose() { }
}
