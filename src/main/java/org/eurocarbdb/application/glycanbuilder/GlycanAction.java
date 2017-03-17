/*
 *   EuroCarbDB, a framework for carbohydrate bioinformatics
 *
 *   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
 *   indicated by the @author tags or express copyright attribution
 *   statements applied by the authors.  
 *
 *   This copyrighted material is made available to anyone wishing to use, modify,
 *   copy, or redistribute it subject to the terms and conditions of the GNU
 *   Lesser General Public License, as published by the Free Software Foundation.
 *   A copy of this license accompanies this distribution in the file LICENSE.txt.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *   for more details.
 *
 *   Last commit: $Rev$ by $Author$ on $Date::             $  
 */

package org.eurocarbdb.application.glycanbuilder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.eurocarbdb.application.glycanbuilder.util.JCommandButtonAction;
import org.eurocarbdb.application.glycanbuilder.util.JCommandMenuButtonAction;
import org.eurocarbdb.application.glycanbuilder.util.JCommandToggleButtonAction;
import org.pushingpixels.flamingo.api.common.CommandButtonDisplayState;
import org.pushingpixels.flamingo.api.common.JCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandToggleButton;
import org.pushingpixels.flamingo.api.common.RichTooltip;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;

/**
 * Implementation of the AbstractAction class that mantains a set of listener to
 * be notified when the action is performed.
 * 
 * @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
 */
public class GlycanAction extends AbstractAction {
	private static final long serialVersionUID = 0L;

	private GlycanAction parent_action = null;
	private ActionListener main_listener = null;
	protected Vector<ActionListener> listeners = new Vector<ActionListener>();

	protected EurocarbResizableIcon eurocarbIcon;

	public EurocarbResizableIcon getEurocarbIcon() {
		return eurocarbIcon;
	}

	public void setEurocarbIcon(EurocarbResizableIcon eurocarbIcon) {
		this.eurocarbIcon = eurocarbIcon;
	}

	protected JCommandButtonAction jCommandButton;
	
	public GlycanAction() {
		this.enableAwareObjects=new ArrayList<Object>();
		this.selectAwareObjects=new ArrayList<Object>();
		
	}

	public GlycanAction(GlycanAction parent, String action, int mnemonic,String accelerator, ActionListener l) {
		this.enableAwareObjects=new ArrayList<Object>();
		this.selectAwareObjects=new ArrayList<Object>();
		init(action + "=" + parent.getActionCommand(), parent.getEurocarbIcon(), parent.getName(), mnemonic, accelerator, l);
		parent_action = parent;
	}

	
	public GlycanAction(String action,EurocarbResizableIcon i,String label, int mnemonic,
			String accelerator, ActionListener l) {
		this.enableAwareObjects=new ArrayList<Object>();
		this.selectAwareObjects=new ArrayList<Object>();
		init(action, i, label, mnemonic, accelerator, l);
	}

	
	public void init(String action, EurocarbResizableIcon i, String label, int mnemonic,
			String accelerator, ActionListener l) {
		setEnabled(true);
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelerator));
	
		putValue(Action.ACTION_COMMAND_KEY, action);
		putValue(Action.MNEMONIC_KEY, mnemonic);
		putValue(Action.NAME, label);
		if (getValue(Action.ACCELERATOR_KEY) == null)
			putValue(Action.SHORT_DESCRIPTION, label);
		else
			putValue(
					Action.SHORT_DESCRIPTION,
					label
							+ "  ["
							+ getAcceleratorText((KeyStroke) getValue(Action.ACCELERATOR_KEY))
							+ "]");
		if (i != null)
			putValue(Action.SMALL_ICON, i.getResizableIcon());
		if (l != null) {
			addActionListener(l);
			main_listener = l;
		}
		
		this.eurocarbIcon=i;
		this.enableAwareObjects=new ArrayList<Object>();
		
		//Should really give calling code control of this, but we need to emulate the old keybindings which were
		//always active for buttons in the toolbars.
		//TODO: Pass control to calling code
		if(l instanceof JComponent || l instanceof JFrame){
			JComponent component=null;
			if(l instanceof JComponent){
				component=(JComponent) l;
			}else{
				component=(JComponent)((JFrame) l).getContentPane();
			}
			
			KeyStroke keyStroke=(KeyStroke) getValue(Action.ACCELERATOR_KEY);
			if(keyStroke!=null && component.getActionForKeyStroke(keyStroke)==null){
				component.registerKeyboardAction(this, (String) getValue(Action.ACTION_COMMAND_KEY), keyStroke,
		                JComponent.WHEN_IN_FOCUSED_WINDOW);
			}
		}
	}


	/**
	 * Return the command run when the action is performed.
	 */
	public String getActionCommand() {
		return (String) getValue(Action.ACTION_COMMAND_KEY);
	}

	/**
	 * Return the action identifier.
	 */
	public String getName() {
		return (String) getValue(Action.NAME);
	}

	/**
	 * Return the label used to display the action in menus.
	 */
	public String getDescription() {
		return (String) getValue(Action.SHORT_DESCRIPTION);
	}

	/**
	 * Return the icon associated with the action.
	 */
	public Icon getIcon() {
		return (Icon) getValue(Action.SMALL_ICON);
	}

	/**
	 * Return the action listener specified when the action was created.
	 */
	public ActionListener getMainListener() {
		return main_listener;
	}

	/**
	 * Return the parent action.
	 */
	public GlycanAction getParentAction() {
		return parent_action;
	}

	private static String getAcceleratorText(KeyStroke accelerator) {
		String acceleratorDelimiter = UIManager
				.getString("MenuItem.acceleratorDelimiter");
		if (acceleratorDelimiter == null)
			acceleratorDelimiter = "+";

		String acceleratorText = "";
		if (accelerator != null) {
			int modifiers = accelerator.getModifiers();
			if (modifiers > 0) {
				acceleratorText = KeyEvent.getKeyModifiersText(modifiers);
				acceleratorText += acceleratorDelimiter;
			}

			int keyCode = accelerator.getKeyCode();
			if (keyCode != 0)
				acceleratorText += KeyEvent.getKeyText(keyCode);
			else
				acceleratorText += accelerator.getKeyChar();
		}
		return acceleratorText;
	}

	/**
	 * Add an action listener.
	 */
	public void addActionListener(ActionListener l) {
		listeners.add(l);
	}

	/**
	 * Remove an action listener.
	 */
	public void removeActionListener(ActionListener l) {
		listeners.remove(l);
	}

	/**
	 * Notify all the listeners that the action has been performed.
	 */
	public void actionPerformed(ActionEvent e) {
		for (Iterator<ActionListener> i = listeners.iterator(); i.hasNext();)
			i.next().actionPerformed(e);
	}

	/**
	 * Return the part of the action identifier before the "=" character.
	 */
	public static String getAction(ActionEvent e) {
		
		String str;
		Object source=e.getSource();
		if(source instanceof HasActionProperty){
			str=((HasActionProperty)source).getActionCommand();
		}else{
			str=  e.getActionCommand();
		}
		
		if (str == null)
			return null;

		int ind = str.indexOf('=');
		if (ind == -1)
			return str;
		return str.substring(0, ind);
	}

	/**
	 * Return the part of the action identifier after the "=" character.
	 */
	public static String getParam(ActionEvent e) {
		String str;
		Object source=e.getSource();
		if(source instanceof HasActionProperty){
			str=((HasActionProperty)source).getActionCommand();
		}else{
			str=  e.getActionCommand();
		}
		if (str == null)
			return null;

		int ind = str.indexOf('=');
		if (ind == -1)
			return null;
		return str.substring(ind + 1);
	}
	
	//JRibbon stuff
	
	protected ActionListener defaultListener;
	
	public ActionListener getDefaultListener() {
		return defaultListener;
	}

	public void setDefaultListener(ActionListener defaultListener) {
		this.defaultListener = defaultListener;
	}

	public JCommandButton getJCommandButton(){
		return this.getJCommandButton(null,(String)getValue(Action.NAME),null,new RichTooltip((String)getValue(Action.NAME),null),true);
	}
	
	public JCommandButton getJCommandButton(ActionListener listener){
		return this.getJCommandButton(null,(String)getValue(Action.NAME),listener,new RichTooltip((String)getValue(Action.NAME),null),true);
	}
	
	public JCommandButton getJCommandButton(ICON_SIZE iconSize,ActionListener listener,String label){
		return this.getJCommandButton(iconSize,label,listener,new RichTooltip(label, " "),true);
	}
	
	public JCommandButton getJCommandButton(ICON_SIZE iconSize,ActionListener listener){
		return this.getJCommandButton(iconSize,(String)getValue(Action.NAME),listener,new RichTooltip((String)getValue(Action.NAME),null),true);
	}
	
	public JCommandButton getJCommandButton(ICON_SIZE iconSize,ActionListener listener,RichTooltip alt){
		return this.getJCommandButton(iconSize,null,listener,alt,true);
	}
	
	public JCommandButton getJCommandButton(String label){
		return getJCommandButton(null,label,null,null,true);
	}
	
	public JCommandButton getJCommandButton(String label,ActionListener listener,RichTooltip alt){
		return getJCommandButton(null,label,listener,alt,true);
	}
	
	public JCommandButton getJCommandButton(ActionListener listener,RichTooltip alt){
		return getJCommandButton(null,null,listener,alt,true);
	}
	
	public JCommandButton getJCommandButton(ICON_SIZE iconSize,String label,ActionListener listener,RichTooltip alt){
		return getJCommandButton(iconSize,label,listener,alt,true);
	}
	
	public JCommandButton getJCommandButton(ICON_SIZE iconSize,String label,ActionListener listener,RichTooltip alt,boolean showIcon){
		JCommandButtonAction jCommandButton;
		
			if(iconSize==null){
				if(showIcon){
					jCommandButton=new JCommandButtonAction(label, this.getEurocarbIcon().getResizableIcon());
				}else{
					jCommandButton=new JCommandButtonAction(label);
				}
				
				jCommandButton.setActionCommand(this.getActionCommand());
				
			}else{
				if(showIcon){
					jCommandButton=new JCommandButtonAction(label, this.getResizableIcon(iconSize));
				}else{
					jCommandButton=new JCommandButtonAction(label);
				}
				
				jCommandButton.setActionCommand(this.getActionCommand());
				//jCommandButton.setActionRichTooltip(alt);
			}
			
			RichTooltip altNew=new RichTooltip();
			
			if(alt==null){
				if(getValue(Action.SHORT_DESCRIPTION)!=null){		
					String name=(String)getValue(Action.NAME);
					if(name==null){
						name=" ";
					}
					altNew.setTitle(name);
				}
			}else{
				altNew.setTitle(alt.getTitle());
				for(String line:altNew.getDescriptionSections()){
					if(!line.matches("^\\\\s+$")){
						altNew.addDescriptionSection(line);
					}
				}
			}
			
			KeyStroke keyStroke=(KeyStroke)getValue(Action.ACCELERATOR_KEY);
			if(keyStroke!=null){
				String keyStrokeString=keyStroke.toString();
				keyStrokeString=keyStrokeString.replace("pressed", "+");
				altNew.addDescriptionSection(keyStrokeString);
			}
			
			jCommandButton.setActionRichTooltip(altNew);
			
			if(listener!=null){
				jCommandButton.addActionListener(listener);
			}
			this.enableAwareObjects.add(jCommandButton);
			
			jCommandButton.registerKeyboardAction(this, "lineup", (KeyStroke)getValue(Action.ACCELERATOR_KEY),
					JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			return jCommandButton;
	}
	
	public JCommandMenuButtonAction getJCommandMenuButton(){
		return this.getJCommandMenuButton(null,(String)getValue(Action.NAME),this.getMainListener(),new RichTooltip((String)getValue(Action.NAME)," "),true);
	}
	
	public JCommandMenuButtonAction getJCommandMenuButton(ICON_SIZE iconSize,String label,ActionListener listener,RichTooltip alt,boolean showIcon){
		JCommandMenuButtonAction jCommandButton;
			if(iconSize==null){
				if(showIcon){
					jCommandButton=new JCommandMenuButtonAction(label, this.getEurocarbIcon().getResizableIcon());
				}else{
					jCommandButton=new JCommandMenuButtonAction(label);
				}
				
				jCommandButton.setActionCommand(this.getActionCommand());
				//jCommandButton.setActionRichTooltip(alt);
			}else{
				if(showIcon){
					jCommandButton=new JCommandMenuButtonAction(label, this.getResizableIcon(iconSize));
				}else{
					jCommandButton=new JCommandMenuButtonAction(label);
				}
				
				jCommandButton.setActionCommand(this.getActionCommand());
				//jCommandButton.setActionRichTooltip(alt);
			}
			
			RichTooltip altNew=new RichTooltip();
			
			if(alt==null){
				if(getValue(Action.SHORT_DESCRIPTION)!=null){		
					String name=(String)getValue(Action.NAME);
					if(name==null){
						name=" ";
					}
					altNew.setTitle(name);
				}
			}else{
				altNew.setTitle(alt.getTitle());
				for(String line:altNew.getDescriptionSections()){
					if(!line.matches("^\\\\s+$")){
						altNew.addDescriptionSection(line);
					}
				}
			}
			
			KeyStroke keyStroke=(KeyStroke)getValue(Action.ACCELERATOR_KEY);
			if(keyStroke!=null){
				String keyStrokeString=keyStroke.toString();
				keyStrokeString=keyStrokeString.replace("pressed", "+");
				altNew.addDescriptionSection(keyStrokeString);
			}
			
			jCommandButton.setActionRichTooltip(altNew);
			
			if(listener!=null){
				jCommandButton.addActionListener(listener);
			}
			this.enableAwareObjects.add(jCommandButton);
			
			jCommandButton.registerKeyboardAction(this, "lineup", (KeyStroke)getValue(Action.ACCELERATOR_KEY),
					JComponent.WHEN_IN_FOCUSED_WINDOW);
			
			return jCommandButton;
	}
	
	
	public JCommandToggleButton getJCommandToggleButton(ActionListener listener,boolean selected){
		return this.getJCommandToggleButton(this.getName(), SwingUtilities.LEFT,listener,selected,null);
	}
	
	public JCommandToggleButton getJCommandToggleButton(String label,ActionListener listener,boolean selected,ICON_SIZE iconSize){
		return this.getJCommandToggleButton(label, SwingUtilities.LEFT,listener,selected,iconSize);
	}
	
	public JCommandToggleButton getJCommandToggleButton(String label,ActionListener listener,boolean selected){
		return this.getJCommandToggleButton(label, SwingUtilities.LEFT,listener,selected,null);
	}
	
	
	
	public JCommandToggleButtonAction getJCommandToggleButton(String label,int aln,ActionListener listener,boolean selected,ICON_SIZE iconSize){
		JCommandToggleButtonAction toggleButton;
		if(this.getEurocarbIcon()!=null && this.getEurocarbIcon().getIconProperties()!=null){
			if(iconSize==null){
				toggleButton=new JCommandToggleButtonAction(label,this.getEurocarbIcon().getResizableIcon());
			}else{
				toggleButton=new JCommandToggleButtonAction(label,this.getResizableIcon(iconSize));
			}
		}else{
			toggleButton=new JCommandToggleButtonAction(label);
		}
	
		RichTooltip altNew=new RichTooltip();
		
		
		if(getValue(Action.SHORT_DESCRIPTION)!=null){		
			String name=(String)getValue(Action.NAME);
			if(name==null){
				name=" ";
			}
			altNew.setTitle(name);
		}
		
		
		KeyStroke keyStroke=(KeyStroke)getValue(Action.ACCELERATOR_KEY);
		if(keyStroke!=null){
			String keyStrokeString=keyStroke.toString();
			keyStrokeString=keyStrokeString.replace("pressed", "+");
			altNew.addDescriptionSection(keyStrokeString);
		}
		
		toggleButton.setActionRichTooltip(altNew);
			
		//toggleButton.setHorizontalAlignment(aln);
		toggleButton.setHorizontalAlignment(SwingUtilities.CENTER);
		toggleButton.setActionCommand(this.getActionCommand());
		toggleButton.addActionListener(listener);
		toggleButton.setDisplayState(CommandButtonDisplayState.TILE);
		if(selected)
			toggleButton.doActionClick();
		this.enableAwareObjects.add(toggleButton);
		
		toggleButton.registerKeyboardAction(this, "lineup", (KeyStroke)getValue(Action.ACCELERATOR_KEY),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		return toggleButton;
	}
	
	public ResizableIcon getResizableIcon(){
		return this.eurocarbIcon.getResizableIcon();
	}
	
	
	/**
	 * Get the icon associated with this GlycanAction scaled to the given icon size.
	 * @param iconSize
	 * @return
	 */
	public ResizableIcon getResizableIcon(ICON_SIZE iconSize){
		if(this.eurocarbIcon.getIconProperties()==null){
			return new ImageResizableIconReducedMem(ThemeManager.getEmptyIcon(iconSize).getImage(),iconSize.getSize(),iconSize.getSize());
		}else{
			return this.eurocarbIcon.getThemeManager().getResizableIcon(this.getEurocarbIcon().getIconProperties().id, iconSize).getResizableIcon();
		}
	}
	
	List<Object> enableAwareObjects;
	List<Object> selectAwareObjects;
	
	public void setSelected(boolean enable){
		super.setEnabled(enable);
		if(this.jCommandButton!=null){
			this.jCommandButton.setEnabled(enable);
		}
		
		for(Object component:selectAwareObjects){
			try {
				Method method=component.getClass().getMethod("setSelected", boolean.class);
				method.invoke(component, enable);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	public void addComponent(Object component){
		this.enableAwareObjects.add(component);
	}
	
	public Object addEnableAware(Object object){
		this.enableAwareObjects.add(object);
		return object;
	}
	public JCheckBox getJCheckBox(String label,ActionListener listener){
		return getJCheckBox(label,false,listener);
	}
	
	public JCheckBox getJCheckBox(String label,boolean setSelected,ActionListener listener){
		JCheckBox checkBox=new JCheckBox(label);
		checkBox.setBorderPainted(false);
		checkBox.setSelected(setSelected);
		checkBox.addActionListener(listener);
		checkBox.setActionCommand(this.getActionCommand());
		this.enableAwareObjects.add(checkBox);
		this.selectAwareObjects.add(checkBox);
		
		return checkBox;
	}
	
	@Override
	public void setEnabled(boolean enable){
		super.setEnabled(enable);
		if(this.jCommandButton!=null){
			this.jCommandButton.setEnabled(enable);
		}
		
		for(Object component:enableAwareObjects){
			try {
				Method method=component.getClass().getMethod("setEnabled", boolean.class);
				method.invoke(component, enable);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
