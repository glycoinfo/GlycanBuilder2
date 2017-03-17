package org.glycoinfo.application.glycanbuilder.dialog;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.eurocarbdb.application.glycanbuilder.CustomFocusTraversalPolicy;
import org.eurocarbdb.application.glycanbuilder.GlycanAction;
import org.eurocarbdb.application.glycanbuilder.util.EscapeDialog;
import org.jdesktop.layout.GroupLayout;

public class ResidueDesignDialog extends EscapeDialog implements ActionListener, ItemListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8327943157144414497L;
	
	public ResidueDesignDialog(Frame parent) {
		super(parent, "Non symbolic monosaccharide list", true);

		initComponents();
		setTraversal();
		setActions();
	
    	setResizable(false);    

    	setLocationRelativeTo(parent);
    }
	
	private void initComponents() {
		this.a_Gro = new JButton();
		this.a_Ery = new JButton();
		this.a_Tho = new JButton();
		this.a_Eru = new JButton();
		this.a_Rul = new JButton();
		this.a_Xul = new JButton();
		this.a_dRib = new JButton();
		this.a_2dPen = new JButton();
		this.a_4dPen = new JButton();
		this.a_4dPenN = new JButton();
		this.a_Ami = new JButton();
		this.a_Asc = new JButton();
		this.a_Boi = new JButton();
		this.a_Rho = new JButton();
		this.a_Sed = new JButton();
		this.a_NeuAcLac = new JButton();
		this.a_NeuGcLac = new JButton();
		this.a_Ko = new JButton();
		this.a_Leg = new JButton();
		this.a_Pse = new JButton();
		this.a_Cym = new JButton();
		this.a_The = new JButton();
		this.a_Aco = new JButton();
		this.a_Ole = new JButton();
		this.a_4eLeg = new JButton();
		this.a_8eLeg = new JButton();
		this.a_HexNA = new JButton();
		this.a_GalNA = new JButton();
		this.a_GlcNA = new JButton();
		this.a_ManNA = new JButton();
		this.a_IdoNA = new JButton();
		this.a_4uHexA = new JButton();
		this.a_4uGalA = new JButton();
		this.a_4uGlcA = new JButton();
		this.a_4uManA = new JButton();
		this.a_4uIdoA = new JButton();
		this.a_Accept = new JButton();
		cancel_button = new JButton();
		jSeparator2 = new JSeparator();
		
		this.a_Triose = new JLabel("Triose");
		this.a_Gro.setText("Gro"); //"Grose"
		this.a_Tetlose = new JLabel("Tetrose");
		this.a_Ery.setText("Ery");//"Erythrose"
		this.a_Tho.setText("Thr");//"Threose"
		this.a_Eru.setText("Eru");//"Erythrulose"
		
		this.a_Pentose = new JLabel("Pentose");
		this.a_Rul.setText("Rul");//Ribulose
		this.a_Xul.setText("Xul");//Xylulose
		this.a_dRib.setText("dRib");//deoxy-ribose
		this.a_2dPen.setText("2dPen");//2-deoxy-Pentose
		this.a_4dPen.setText("4dPen");//4-deoxy-Pentose
		this.a_4dPenN.setText("4dPenN");//4-deoxy-PenN
		
		this.a_Hexose = new JLabel("Hexose");
		this.a_Ami.setText("Ami");//Amicetose
		this.a_Asc.setText("Asc");//Ascarilose
		this.a_Boi.setText("Boi");//Boivinose
		this.a_Rho.setText("Rho");//Rhodinose
		
		this.a_HexoseNA = new JLabel("Acidic 2-deoxy-amine sugars");
		this.a_HexNA.setText("HexNA");
		this.a_GlcNA.setText("GlcNA");
		this.a_GalNA.setText("GalNA");
		this.a_ManNA.setText("ManNA");
		this.a_IdoNA.setText("IdoNA");
		
		this.a_4uHexose = new JLabel("Unsaturated Hexuronic acid");
		this.a_4uHexA.setText("4uHexA");
		this.a_4uGlcA.setText("4uGlcA");
		this.a_4uGalA.setText("4uGalA");
		this.a_4uManA.setText("4uManA");
		this.a_4uIdoA.setText("4uIdoA");
		
		this.a_Heptose = new JLabel("Heptose");
		this.a_Sed.setText("Sed");//Sedoheptulose
				
		this.a_Nonase = new JLabel("Nonase");
		this.a_NeuAcLac.setText("NeuAcLac");//NeuAcLac
		this.a_NeuGcLac.setText("NeuGcLac");//NeuGcLac
		this.a_Ko.setText("Ko"); //Ketooctonic acid
		this.a_Leg.setText("Leg");//Legionaminic acid
		this.a_Pse.setText("Pse");//Pseudaminic acid
		this.a_4eLeg.setText("4eLeg");
		this.a_8eLeg.setText("8eLeg");
		
		this.a_Others = new JLabel("Others");
		this.a_Cym.setText("Cym"); //Cymarose
		this.a_The.setText("The"); //Thevetose
		this.a_Aco.setText("Aco"); //Acofriose
		this.a_Ole.setText("Ole"); //Oleandrose
		
		this.a_TrivalField = new JLabel("String name");
		this.a_textBox = new JTextField();
		this.a_Accept.setText("Accept");
				
		cancel_button.setText("Cancel");
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				closeDialog();
			}
		});
		
    	GroupLayout layout = new GroupLayout(this.getContentPane());
    	this.getContentPane().setLayout(layout);
    	layout.setAutocreateGaps(true);
        layout.setAutocreateContainerGaps(true);
    	
        layout.setHorizontalGroup(
        		layout.createParallelGroup(GroupLayout.CENTER)
        		.add(layout.createSequentialGroup()
        				//.addContainerGap()
        				.add(layout.createParallelGroup(GroupLayout.CENTER)
        						.add(jSeparator2, GroupLayout.DEFAULT_SIZE, 10, Short.MAX_VALUE)
        						.add(layout.createSequentialGroup()
        								.add(10, 10, 10)
        								.add(cancel_button))
        						.add(layout.createParallelGroup(GroupLayout.CENTER)
        								.add(this.a_Triose)
        								.add(this.a_Tetlose)
        								.add(this.a_Pentose)
        								.add(this.a_Hexose)
        								.add(this.a_HexoseNA)
        								.add(this.a_4uHexose)
        								.add(this.a_Heptose)
        								.add(this.a_Nonase)
        								.add(this.a_Others)
        								/*.add(this.a_TrivalField)*/)
        						)
        				.add(layout.createParallelGroup()
        						.add(this.a_Gro)
        						.add(this.a_Eru)
        						.add(this.a_Xul)
        						.add(this.a_2dPen)
        						.add(this.a_Ami)
        						.add(this.a_Boi)
        						.add(this.a_HexNA)
        						.add(this.a_ManNA)
        						.add(this.a_4uHexA)
        						.add(this.a_4uManA)
        						.add(this.a_Sed)
        						.add(this.a_NeuAcLac)
        						.add(this.a_Leg)
        						.add(this.a_4eLeg)
        						.add(this.a_Aco)
        						.add(this.a_Ole)
        						/*.add(this.a_textBox)*/)
        				.add(layout.createParallelGroup()
        						.add(this.a_Ery)
        						.add(this.a_Rul)
        						.add(this.a_4dPen)
        						.add(this.a_Asc)
        						.add(this.a_GlcNA)
        						.add(this.a_IdoNA)
        						.add(this.a_4uGlcA)
        						.add(this.a_4uIdoA)
        						.add(this.a_NeuGcLac)
        						.add(this.a_Pse)
        						.add(this.a_8eLeg)
        						.add(this.a_Cym)
        						/*.add(this.a_Accept)*/)
        				.add(layout.createParallelGroup()
        						.add(this.a_Tho)
        						.add(this.a_4dPenN)
        						.add(this.a_dRib)
        						.add(this.a_Rho)
        						.add(this.a_GalNA)
        						.add(this.a_4uGalA)
        						.add(this.a_Ko)
        						.add(this.a_The))
        				.addContainerGap())
        		);
        
		layout.linkSize(new Component[] {cancel_button}, GroupLayout.HORIZONTAL);
     
		layout.setVerticalGroup(
				layout.createParallelGroup(GroupLayout.CENTER)
				.add(layout.createSequentialGroup()
						//.addContainerGap()
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Triose)
								.add(this.a_Gro))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Tetlose)
								.add(this.a_Eru)
								.add(this.a_Ery)
								.add(this.a_Tho))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Pentose)
								.add(this.a_Xul)
								.add(this.a_Rul)
								.add(this.a_dRib))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_2dPen)
								.add(this.a_4dPen)
								.add(this.a_4dPenN))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Hexose)
								.add(this.a_Ami)
								.add(this.a_Asc)
								.add(this.a_Rho))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Boi))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_HexoseNA)
								.add(this.a_HexNA)
								.add(this.a_GlcNA)
								.add(this.a_GalNA))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_ManNA)
								.add(this.a_IdoNA))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_4uHexose)
								.add(this.a_4uHexA)
								.add(this.a_4uGlcA)
								.add(this.a_4uGalA))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_4uManA)
								.add(this.a_4uIdoA))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Heptose)
								.add(this.a_Sed))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Nonase)
								.add(this.a_NeuAcLac)
								.add(this.a_NeuGcLac)
								.add(this.a_Ko))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Leg)
								.add(this.a_Pse))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_4eLeg)
								.add(this.a_8eLeg))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Others)
								.add(this.a_Aco)
								.add(this.a_Cym)
								.add(this.a_The))
						.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_Ole))
						.add(jSeparator2, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
						/*.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(this.a_TrivalField)
								.add(this.a_Accept)
								.add(this.a_textBox))
						*/.add(layout.createParallelGroup(GroupLayout.CENTER)
								.add(cancel_button))
						.addContainerGap())
				);
        
		layout.linkSize(new Component[] {cancel_button}, GroupLayout.VERTICAL);
		pack();
    }
	
	private void setTraversal() {
		CustomFocusTraversalPolicy tp = new CustomFocusTraversalPolicy();

		this.setFocusTraversalPolicy(tp);
		tp.addComponent(cancel_button);
		tp.addComponent(this.a_Gro);
		tp.addComponent(this.a_Ery);
		tp.addComponent(this.a_Tho);
		tp.addComponent(this.a_Eru);
		tp.addComponent(this.a_Rul);
		tp.addComponent(this.a_Xul);
		tp.addComponent(this.a_dRib);
		tp.addComponent(this.a_2dPen);
		tp.addComponent(this.a_4dPen);
		tp.addComponent(this.a_4dPenN);
		tp.addComponent(this.a_Ami);
		tp.addComponent(this.a_Asc);
		tp.addComponent(this.a_Boi);
		tp.addComponent(this.a_Rho);
		tp.addComponent(this.a_Sed);
		tp.addComponent(this.a_NeuAcLac);
		tp.addComponent(this.a_NeuGcLac);
		tp.addComponent(this.a_Ko);
		tp.addComponent(this.a_Leg);
		tp.addComponent(this.a_Pse);
		tp.addComponent(this.a_Cym);
		tp.addComponent(this.a_The);
		tp.addComponent(this.a_Aco);
		tp.addComponent(this.a_Ole);
		tp.addComponent(this.a_4eLeg);
		tp.addComponent(this.a_8eLeg);
		tp.addComponent(this.a_HexNA);
		tp.addComponent(this.a_GalNA);
		tp.addComponent(this.a_GlcNA);
		tp.addComponent(this.a_ManNA);
		tp.addComponent(this.a_IdoNA);
		tp.addComponent(this.a_4uHexA);
		tp.addComponent(this.a_4uGalA);
		tp.addComponent(this.a_4uGlcA);
		tp.addComponent(this.a_4uManA);
		tp.addComponent(this.a_4uIdoA);
		//tp.addComponent(this.a_Accept);
		
		getRootPane().setDefaultButton(cancel_button);
	}
	
	private void setActions() {
		this.a_Gro.addActionListener(this);
		this.a_Ery.addActionListener(this);
		this.a_Tho.addActionListener(this);
		this.a_Eru.addActionListener(this);
		this.a_Rul.addActionListener(this);
		this.a_Xul.addActionListener(this);
		this.a_dRib.addActionListener(this);
		this.a_2dPen.addActionListener(this);
		this.a_4dPen.addActionListener(this);
		this.a_4dPenN.addActionListener(this);
		this.a_Ami.addActionListener(this);
		this.a_Asc.addActionListener(this);
		this.a_Boi.addActionListener(this);
		this.a_Rho.addActionListener(this);
		this.a_Sed.addActionListener(this);
		this.a_NeuAcLac.addActionListener(this);
		this.a_NeuGcLac.addActionListener(this);
		this.a_Ko.addActionListener(this);
		this.a_Leg.addActionListener(this);
		this.a_Pse.addActionListener(this);
		this.a_Cym.addActionListener(this);
		this.a_The.addActionListener(this);
		this.a_Aco.addActionListener(this);
		this.a_Ole.addActionListener(this);
		this.a_4eLeg.addActionListener(this);
		this.a_8eLeg.addActionListener(this);
		this.a_HexNA.addActionListener(this);
		this.a_GlcNA.addActionListener(this);
		this.a_GalNA.addActionListener(this);
		this.a_ManNA.addActionListener(this);
		this.a_IdoNA.addActionListener(this);
		this.a_4uHexA.addActionListener(this);
		this.a_4uGlcA.addActionListener(this);
		this.a_4uGalA.addActionListener(this);
		this.a_4uManA.addActionListener(this);
		this.a_4uIdoA.addActionListener(this);
		//this.a_Accept.addActionListener(this);
		cancel_button.addActionListener(this);
	}
	
	public boolean isCanceled() {
		return return_status.equals("Cancel");
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = GlycanAction.getAction(e);

		if( !action.equals("Cancel") && !action.equals("Accept")) {
			return_status = action;
			closeDialog();
		}
		if( action.equals("Cancel") ) {
			return_status = action;
			closeDialog();
		}
		/*if( action.equals("Accept")) {
			return_status = action;
			a_strRC = this.a_textBox.getText();
			closeDialog();
		}*/
	}
	
	//public String getResidueCode() {
	//	return this.a_strRC;
	//}
	
	public String getReturnStatus() {
		return this.return_status;
	}
	
	private void closeDialog() {
		setVisible(false);
		dispose();
	}

	private String a_strRC;
	
    private JButton cancel_button;
    
    private JLabel a_Triose;
	private JButton a_Gro;
	
	private JLabel a_Tetlose;
	private JButton a_Ery;
	private JButton a_Tho;
	private JButton a_Eru;
	
	private JLabel a_Pentose;
	private JButton a_Rul;
	private JButton a_Xul;
	private JButton a_2dPen;
	private JButton a_4dPen;
	private JButton a_dRib;
	private JButton a_4dPenN;
	
	private JLabel a_Hexose;
	private JButton a_Ami;
	private JButton a_Asc;
	private JButton a_Boi;
	private JButton a_Rho;

	private JLabel a_HexoseNA;
	private JButton a_HexNA;
	private JButton a_GalNA;
	private JButton a_GlcNA;
	private JButton a_IdoNA;
	private JButton a_ManNA;
	
	private JLabel a_4uHexose;
	private JButton a_4uHexA;
	private JButton a_4uGlcA;
	private JButton a_4uGalA;
	private JButton a_4uManA;
	private JButton a_4uIdoA;
	
	private JLabel a_Heptose;
	private JButton a_Sed;	
	
	private JLabel a_Nonase;
	private JButton a_NeuAcLac;
	private JButton a_NeuGcLac;
	private JButton a_Ko;
	private JButton a_Leg;
	private JButton a_Pse;
	private JButton a_4eLeg;
	private JButton a_8eLeg;
	
	private JLabel a_Others;
	private JButton a_The;
	private JButton a_Aco;
	private JButton a_Cym;
	private JButton a_Ole;
	
	private JLabel a_TrivalField;
	private JTextField a_textBox;
	private JButton a_Accept;
	
	private JSeparator jSeparator2;
}
