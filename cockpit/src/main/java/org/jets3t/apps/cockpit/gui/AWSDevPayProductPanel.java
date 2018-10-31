/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 Zmanda Inc, 2008 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.apps.cockpit.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jets3t.service.model.AWSDevPayProduct;

/**
 * Class to store information about a AWS DevPay product.
 *
 * @author Nikolas Coukouma
 * @author James Murty
 */
public class AWSDevPayProductPanel extends JPanel {
    private static final long serialVersionUID = -5192203961525549067L;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);

    private JRadioButton awsProductRadioButton = null;
    private JComboBox awsProductListComboBox = null;
    private JRadioButton awsProductTokenRadioButton = null;
    private JTextField awsProductTokenTextField = null;

    private Component[] internalComponents = null;

    public AWSDevPayProductPanel() {
        super(new GridBagLayout());
        initGui();
    }

    private void initGui() {
        String awsProductRadioButtonText =
            "DevPay Product";
        String awsProductTokenRadioButtonText =
            "DevPay Product Token";
        String awsProductTokenTooltipText =
            "DevPay product token";

        awsProductRadioButton = new JRadioButton(awsProductRadioButtonText);
        try {
            awsProductListComboBox = new JComboBox(AWSDevPayProduct.load());
        } catch(Exception e) {
            awsProductListComboBox = new JComboBox();
        }
        awsProductTokenRadioButton = new JRadioButton(awsProductTokenRadioButtonText);
        awsProductTokenTextField = new JTextField();
        awsProductTokenTextField.setToolTipText(awsProductTokenTooltipText);
        ButtonGroup productTokenGroup = new ButtonGroup();
        productTokenGroup.add(awsProductRadioButton);
        productTokenGroup.add(awsProductTokenRadioButton);

        Component[] tmp = {awsProductRadioButton, awsProductTokenRadioButton, awsProductListComboBox};
        internalComponents = tmp;
        if (awsProductListComboBox.getItemCount() == 0) {
            awsProductTokenRadioButton.setSelected(true);

            awsProductRadioButton.setVisible(false);
            awsProductListComboBox.setVisible(false);
        } else {
            awsProductRadioButton.setSelected(true);
        }

        int row = 0;
        add(awsProductRadioButton, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        add(awsProductListComboBox, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(awsProductTokenRadioButton, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        add(awsProductTokenTextField, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (awsProductListComboBox.getItemCount() == 0) {
            enabled = false;
        }
        for (int i = 0; i < internalComponents.length; i++) {
            internalComponents[i].setEnabled(enabled);
        }
    }

    /**
     * @return
     * the product token provided, or selected, by the user.
     */
    public String getAWSProductToken() {
        if (awsProductTokenRadioButton.isSelected()) {
            return awsProductTokenTextField.getText().trim();
        } else {
            return ((AWSDevPayProduct) awsProductListComboBox.getSelectedItem()).getProductToken().trim();
        }
    }
}
