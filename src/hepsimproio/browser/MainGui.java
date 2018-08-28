package hepsimproio.browser;

/**
 * Main GUI Class for the ProIO browser.
 * This program is open source and licensed using the GNU Public license
 * 
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import hepsimproio.browser.editor.SimpleEditor;
import proio.Reader;
import proio.Event;
import hepsimproio.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;



/**
 * Main class for the ProIO browser of EVGEN HepSim events.
 * 
 * @author S.Chekanov
 * 
 */
public class MainGui {

	private JFrame frame;
	private JMenuBar menuBar;
	private JSplitPane jSplit;
	private JScrollPane jPan1;
	private MainPanel jPan2;
	private JPanel jPanelStatus;
	private MemoryMonitor memMon;
	private JList<Object> listevents;
	private JPopupMenu popupMenu;
	private JMenuItem jpop1, jpop2;
	private ArrayList<String> events;
	private hepsimproio.FileMC file;
	private proio.Reader reader;
	private JTable table;
	private Map<Integer, Double> mass_map = new HashMap<Integer, Double>();
	private Map<Integer, String> name_map = new HashMap<Integer, String>();
	private Map<Integer, Integer> charge_map = new HashMap<Integer, Integer>();
	private MyTableModelInfo model;
	private JLabel statusBar;
	private long version = -1;
	private String current_event = "";
	private String mess = "";
	private long ev_current = -1;;
	private String protofile="";
	private String logfile="";
	private long maxevents=0;
	private Map<String, ByteString> metadata;
	private String filename="";
	private float unit=0;
	private float lunit=0;
	private String ptag="Particle";

	/**
	 * Build a frame and read the file.
	 * 
	 * @param filename 
	 *            Input ProIO File
	 **/
	public MainGui(String filename) {

		this.filename=filename;
		current_event = "";
		model = new MyTableModelInfo();

		table = new JTable(model) {
			        public static final long serialVersionUID = 1;

			        public Component prepareRenderer(TableCellRenderer renderer,
			                                         int rowIndex, int vColIndex) {
				        Component c = super.prepareRenderer(renderer, rowIndex,
				                                            vColIndex);
				        Color colcell = new Color(245, 255, 255);
				        if (rowIndex % 2 == 0 && !isCellSelected(rowIndex, vColIndex)) {
					        c.setBackground(colcell);
				        } else {
					        // If not shaded, match the table's background
					        c.setBackground(getBackground());
				        }
				        // column
				        c.setForeground(Color.black);
				        if (isCellSelected(rowIndex, vColIndex)) {
					        c.setBackground(Color.blue);
					        c.setForeground(Color.white);
				        }
				        return c;
			        }
		        };


		table.setFillsViewportHeight(true);
		// table.setAutoCreateRowSorter(true);


		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(true);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);

		listevents = new JList<Object>();
		MouseListener mouseListener = new MouseAdapter() {
			                              public void mouseClicked(MouseEvent mouseEvent) {
				                              JList theList = (JList) mouseEvent.getSource();

				                              if (SwingUtilities.isRightMouseButton(mouseEvent)
				                                              && !theList.isSelectionEmpty()
				                                              && theList.locationToIndex(mouseEvent.getPoint()) == theList
				                                              .getSelectedIndex()) {
					                              int index = theList.locationToIndex(mouseEvent.getPoint());
					                              Object o = theList.getModel().getElementAt(index);
					                              String key = o.toString();
					                              ev_current = Long.parseLong(key) - 1;
					                              popupMenu.show(theList, mouseEvent.getX(),
					                                             mouseEvent.getY());
				                              }

				                              if (mouseEvent.getClickCount() == 2) {
					                              int index = theList.locationToIndex(mouseEvent.getPoint());
					                              if (index >= 0) {
						                              Object o = theList.getModel().getElementAt(index);
						                              String key = o.toString();
						                              ev_current = Long.parseLong(key) - 1;
						                              current_event = "  Event=" + key;
						                              showEventParticles( Long.parseLong(key) );
					                              }
				                              }
			                              };
		                              };

		listevents.addMouseListener(mouseListener);

		jpop1 = new JMenuItem("MCParameters");
		jpop2 = new JMenuItem("Particles");
		popupMenu = new JPopupMenu();
		popupMenu.add(jpop1);
		popupMenu.add(jpop2);
		jpop1.addActionListener(new ActionListener() {
			                        public void actionPerformed(ActionEvent ev) {
				                        statusBar.setText("MCParameters");
				                        current_event = " Event=" + Long.toString(ev_current + 1);
				                        showEventInfo( ev_current + 1);
			                        }
		                        });

		jpop2.addActionListener(new ActionListener() {
			                        public void actionPerformed(ActionEvent ev) {
				                        statusBar.setText("Particles");
				                        current_event = " Event=" + Long.toString(ev_current + 1);
				                        showEventParticles( (ev_current + 1) );
			                        }
		                        });

		statusBar = new JLabel("No file");
		if (filename != null)
			openFile(filename);

		frame = new JFrame("ProIO Browser");
		if (filename != null)
			frame.setTitle("File: "+filename);

		Dimension res = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension size = new Dimension(Math.min(400, res.width), Math.min(400,
		                               res.height));
		frame.setSize(size);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Creates a menubar for a JFrame
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		JMenu aboutMenu = new JMenu("Help");
		JMenu metaMenu = new JMenu("MetaData");
		JMenu protoMenu = new JMenu("Data layout");

		menuBar.add(fileMenu);
		menuBar.add(metaMenu);
		menuBar.add(protoMenu);
		menuBar.add(aboutMenu);

		JMenuItem item00 = new JMenuItem(new OpenAction());
		fileMenu.add(item00);
		JMenuItem item01 = new JMenuItem(new ExitAction());
		fileMenu.add(item01);
		JMenuItem item11 = new JMenuItem(new ShowAboutAction());
		aboutMenu.add(item11);
		JMenuItem item21 = new JMenuItem(new ShowHeaderAction());
		metaMenu.add(item21);
		JMenuItem item24 = new JMenuItem(new ShowLogFileAction());
		metaMenu.add(item24);
		JMenuItem item33 = new JMenuItem(new ShowEventProtoAction());
		protoMenu.add(item33);
		jSplit = new JSplitPane();
		jSplit.setDividerLocation(0.3);

		jPan1 = new JScrollPane(listevents);
		jPan1.setPreferredSize(new java.awt.Dimension(60, 400));
		jPan1.setMinimumSize(new java.awt.Dimension(60, 400));

		jPan2 = new MainPanel(table);
		jSplit.setLeftComponent(jPan1);
		jSplit.setRightComponent(jPan2);

		memMon = new MemoryMonitor();
		memMon.setPreferredSize(new java.awt.Dimension(65, 18));
		memMon.setMinimumSize(new java.awt.Dimension(23, 10));

		jPanelStatus = new JPanel();
		jPanelStatus.setPreferredSize(new Dimension(700, 20));
		jPanelStatus.setLayout(new BorderLayout());
		jPanelStatus.setBorder(new javax.swing.border.EtchedBorder());
		jPanelStatus.add(statusBar, BorderLayout.WEST);
		jPanelStatus.add(memMon, BorderLayout.EAST);

		frame.getContentPane().add(jSplit, java.awt.BorderLayout.CENTER);
		frame.getContentPane().add(jPanelStatus, java.awt.BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);

	}


	// get barcode ID
	private static int getID(Object obj) {
		int barcode=0;
		if (obj instanceof proio.model.Mc.Particle) {
			proio.model.Mc.Particle par = (proio.model.Mc.Particle) obj;
			barcode=par.getId();
		};
		return barcode;

	}


	// open file
	private void openFile(String f) {

		file = new hepsimproio.FileMC(f);
		reader = file.reader();
		version = 1;

		try {
			name_map=hepsimproio.HepSim.GetPDGNames();
		} catch (Throwable e) {
			System.out.println(e);
		}



		int nn=0;
		Event storeevent = null;
		Event tmpEvent;
		while (true) {
			tmpEvent = null;
			try {
				tmpEvent = reader.next(true);
			} catch (Throwable e) {
				;
			}
			if (tmpEvent == null) {
				break;
			}
			storeevent = tmpEvent;
			nn++;
		}

		// last event has metadata in it!
		maxevents=nn-1;
		model = new MyTableModelInfo();
		model.addColumn("Description");
		model.addColumn("Value");



		Map<String, String> meta = new HashMap<>();
		metadata = storeevent.getMetadata();
		for (Map.Entry<String, ByteString> entry : metadata.entrySet()) {
			String skey=entry.getKey();
			String name=entry.getValue().toStringUtf8();
			if (skey.equals("meta:model.proto")) protofile=name;
			if (skey.equals("meta:logfile")) logfile=name;
			if (skey.equals("info:varint_energy")) {
				unit = (float)Double.parseDouble(name);
			}

			if (skey.equals("info:varint_length")) {
				lunit = (float)Double.parseDouble(name);
			}

			if (!name.equals("logfile") && !skey.equals("meta:model.proto") &&  !skey.equals("meta:logfile")) {
				meta.put(new String(skey),new String(name));
			}
		};

		// sort metadata
		SortedSet<String> keys = new TreeSet<>(meta.keySet());
		for (String key : keys) {
			String value = meta.get(key);
			model.addRow(new Object[] { new String(key), new String(value)});
		}




		table.setModel(model);

		events = new ArrayList<String>();
		for (long i = 0; i < maxevents; i++) {
			events.add(new String(Long.toString(i + 1)));
		}

		statusBar.setText(mess);
		listevents.setListData(events.toArray());

	}

	// formatted output
	private Double getDouble(double d) {

		if (d != 0) {
			return new Double(d);
		}
		return -1.0;
	}

	// formatted output
	private Integer getInt(int d) {

		if (d != 0) {
			return new Integer(d);
		}
		return -1;
	}

	// formatted output
	private Long getLong(long d) {

		if (d != 0) {
			return new Long(d);
		}
		return -1L;
	}

	// formatted output
	private String getString(String d) {

		if (d != null) {
			return new String(d);
		}
		return " ";
	}

	// show particle info
	private void showEventInfo(long nevent) {

		statusBar.setText("Tag: "+ptag+" for event Nr=" + nevent);
		model = new MyTableModelInfo();
		model.addColumn("entryID");
		model.addColumn("Value");



		// check if the record starts from 0
		try {
			reader.seekToStart();
			reader.skip(nevent-1);
		} catch (IOException e) {
			System.out.println(e);
		}



		for (Event event : reader) {

			for (long entryID : event.getTaggedEntries("MCParameters")) {

				String txt="";
				try{
					txt= event.getEntry(entryID).toString();
				} catch (Throwable e) {
					;
				}
				String[] data=txt.split("\n");
				for (int i=0; i<data.length; i++){
					model.addRow(new Object[] { new String("MCParameters = "+Long.toString(entryID)),  new String(data[i]) });
				};

			}
			break;
		}

		table.setModel(model);

	}

	// show particle info
	private void showEventParticles(long nevent) {

		model = new MyTableModelInfo();
		model.addColumn("No");
		model.addColumn("Name");
		model.addColumn("PID");
		model.addColumn("Status");
		model.addColumn("M1");
		model.addColumn("M2");
		model.addColumn("D1");
		model.addColumn("D2");
		model.addColumn("Px (GeV)");
		model.addColumn("Py (GeV)");
		model.addColumn("Pz (GeV)");
		model.addColumn("E (GeV)");
		model.addColumn("M (GeV)");
		model.addColumn("X (mm)");
		model.addColumn("Y (mm)");
		model.addColumn("Z (mm)");
		model.addColumn("T (mm/c)");
		model.addColumn("BarCode");
		model.setRowCount(0);
		table.setModel(model);



		// check if the record starts from 0


		try {
			reader.seekToStart();
			reader.skip(nevent-1);
		} catch (IOException e) {
			System.out.println(e);
		}



		for (Event event : reader) {

			for (long entryID : event.getTaggedEntries("VarintPackedParticles")) {
				//System.out.println(entryID);
				proio.model.Mc.VarintPackedParticles par = null;
				try {
					Object obj = event.getEntry(entryID);
					par = (proio.model.Mc.VarintPackedParticles) obj;
				} catch (Throwable e) {
					;
				}
				ptag="VarintPackedParticles";

				if (unit==0 || lunit==0) {
					JOptionPane.showMessageDialog(frame,
					                              "Found VarintPackedParticles, but varint units \"info:varint_energy\" and \"info:varint_length\" are not set!", "Error",
					                              JOptionPane.ERROR_MESSAGE);

					System.exit(0);
				}

				int nn=0;
				for (int jj=0; jj<(int)par.getPdgCount();  jj++){
					int barcode=   par.getBarcode(jj);
					int id=   par.getId(jj);
					int pid = par.getPdg(jj);
					int status=par.getStatus(jj);
					double m =par.getMass(jj)/unit;
					float px = par.getPx(jj)/unit;
					float py = par.getPy(jj)/unit;
					float pz = par.getPz(jj)/unit;
					float ee = par.getEnergy(jj)/unit;
					int m1=par.getParent1(jj);
					int m2=par.getParent2(jj);
					int d1=par.getChild1(jj);
					int d2=par.getChild2(jj);
					float x=par.getX(jj)/lunit;
					float y=par.getY(jj)/lunit;
					float z=par.getZ(jj)/lunit;
					float t=par.getT(jj)/lunit;
					String name = "none";
					if (name_map.containsKey(pid))
						name = name_map.get(pid);

					nn++;
					model.addRow(new Object[] { new Integer(id),
					                            new String(name), new Integer(pid),
					                            new Integer(status), new Integer(m1), new Integer(m2),
					                            new Integer(d1), new Integer(d2), new Double(px),
					                            new Double(py), new Double(pz), new Double(ee), new Double(m),
					                            new Double(x), new Double(y), new Double(z),
					                            new Double(t), new Integer(barcode) });

				}

			}; // end of varintPacketparticles



			// PackedParticles
			for (long entryID : event.getTaggedEntries("PackedParticles")) {
				//System.out.println(entryID);
				proio.model.Mc.PackedParticles par = null;
				try {
					Object obj = event.getEntry(entryID);
					par = (proio.model.Mc.PackedParticles) obj;
				} catch (Throwable e) {
					;
				}
				ptag="PackedParticles";
				int nn=0;
				for (int jj=0; jj<(int)par.getPdgCount();  jj++){
					int barcode=   par.getBarcode(jj);
					int id=   par.getId(jj);
					int pid = par.getPdg(jj);
					int status=par.getStatus(jj);
					double m =par.getMass(jj);
					float px = par.getPx(jj);
					float py = par.getPy(jj);
					float pz = par.getPz(jj);
					float ee = par.getEnergy(jj);
					int m1=par.getParent1(jj);
					int m2=par.getParent2(jj);
					int d1=par.getChild1(jj);
					int d2=par.getChild2(jj);
					float x=par.getX(jj);
					float y=par.getY(jj);
					float z=par.getZ(jj);
					float t=par.getT(jj);
					String name = "none";
					if (name_map.containsKey(pid))
						name = name_map.get(pid);

					nn++;
					model.addRow(new Object[] { new Integer(id),
					                            new String(name), new Integer(pid),
					                            new Integer(status), new Integer(m1), new Integer(m2),
					                            new Integer(d1), new Integer(d2), new Double(px),
					                            new Double(py), new Double(pz), new Double(ee), new Double(m),
					                            new Double(x), new Double(y), new Double(z),
					                            new Double(t), new Integer(barcode) });

				}

			}; // end of Packetparticles






			for (long entryID : event.getTaggedEntries("Particle")) {
				int nn=0;
				try {


					Object obj = event.getEntry(entryID);
					if (obj instanceof proio.model.Mc.Particle) {
						proio.model.Mc.Particle par = (proio.model.Mc.Particle) obj;

						proio.model.Mc.XYZTF vertex=par.getVertex();
						proio.model.Mc.XYZF p=par.getP();

						int barcode=   par.getBarcode();
						int id=   par.getId();
						int pid = par.getPdg();
						int status=par.getStatus();
						double m =par.getMass();
						float px = p.getX();
						float py = p.getY();
						float pz = p.getZ();
						float ee = par.getEnergy();
						int m1=0;
						int m2=0;
						for (int j=0; j<par.getParentCount(); j++){
							if (j==0) m1=getID(event.getEntry(par.getParent(j)));
							if (j==1) m2=getID(event.getEntry(par.getParent(j)));
						};

						int d1=0;
						int d2=0;

						for (int j=0; j<par.getChildCount(); j++){
							if (j==0) d1=getID(event.getEntry(par.getChild(j)));
							if (j==1) d2=getID(event.getEntry(par.getChild(j)));
						};


						float x=vertex.getX();
						float y=vertex.getY();
						float z=vertex.getZ();
						float t=vertex.getT();


						String name = "none";
						if (name_map.containsKey(pid))
							name = name_map.get(pid);


						nn++;
						model.addRow(new Object[] { new Integer(id),
						                            new String(name), new Integer(pid),
						                            new Integer(status), new Integer(m1), new Integer(m2),
						                            new Integer(d1), new Integer(d2), new Double(px),
						                            new Double(py), new Double(pz), new Double(ee), new Double(m),
						                            new Double(x), new Double(y), new Double(z),
						                            new Double(t), new Integer(barcode) });

					} // end MC particle

				} catch (Throwable e) {
					;
				}


			} // end eparticle

			break; // break event
		}

		statusBar.setText("Tag: \""+ptag+"\" for event Nr=" + Long.toString(nevent));

	}

	private class ShowAboutAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ShowAboutAction() {
			super("About");
		}

		public void actionPerformed(ActionEvent e) {
			new AboutDialog(frame);
		}
	}

	private class ExitAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ExitAction() {
			super("Exit");
		}

		public void actionPerformed(ActionEvent e) {
			frame.setVisible(false);
			frame.dispose();

		}
	}

	private class OpenAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		OpenAction() {
			super("Open file");
		}

		public void actionPerformed(ActionEvent e) {

			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
			FileNameExtensionFilter filterm3u = new FileNameExtensionFilter(
			                                            "ProIO file (.proio)", "proio");
			chooser.addChoosableFileFilter(filterm3u);

			try {
				int returnVal = chooser.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					String fullPath = chooser.getSelectedFile()
					                  .getAbsolutePath();
					if (fullPath != null)
						openFile(fullPath);
				}

			} catch (Exception f) {
				f.printStackTrace();
				System.out.println("Error saving file.");
			}

		}
	}

	private class ShowHeaderAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ShowHeaderAction() {
			super("Header Record");
		}

		public void actionPerformed(ActionEvent e) {

			model = new MyTableModelInfo();
			model.addColumn("Description");
			model.addColumn("Value");
			for (Map.Entry<String, ByteString> entry : metadata.entrySet()) {
				String skey=entry.getKey();
				String name=entry.getValue().toStringUtf8();
				if (skey.equals("meta:model.proto")) protofile=name;
				if (skey.equals("meta:logfile")) logfile=name;
				if (!name.equals("logfile") && !skey.equals("meta:model.proto") &&  !skey.equals("meta:logfile")) {
					model.addRow(new Object[] { new String(skey), new String(name)});
				}
			};


			table.setModel(model);

		}
	};

	private class ShowParticleDataAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ShowParticleDataAction() {
			super("Particle Data");
		}

		public void actionPerformed(ActionEvent e) {

			model = new MyTableModelInfo();
			model.addColumn("Nr");
			model.addColumn("PID");
			model.addColumn("Name");
			model.addColumn("Mass (GeV)");
			model.addColumn("Charge");
			model.addColumn("Lifetime");
			/*
			                        if (header != null) {
						for (int j = 0; j < header.getParticleDataCount(); j++) {
							promc.io.ProMCHeaderFile.ProMCHeader.ParticleData d = header
									.getParticleData(j);
							String name = d.getName();
			                                int charge=d.getCharge();
							model.addRow(new Object[] { new Integer(j + 1),
									new Integer(d.getId()), new String(name),
									new Double(d.getMass()), new Integer(charge), new Double(d.getLifetime()) });
						}
						table.setModel(model);
			                        }

			*/

		}
	}

	private class ShowStatisticsAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ShowStatisticsAction() {
			super("Statistics");
		}

		public void actionPerformed(ActionEvent e) {

			model = new MyTableModelInfo();
			model.addColumn("Description");
			model.addColumn("Value");

			table.setModel(model);

		}
	}


	/**
	 * Show ProMC.proto
	 * 
	 * @author sergei
	 * 
	 */
	private class ShowEventProtoAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ShowEventProtoAction() {
			super("model.proto");
		}

		public void actionPerformed(ActionEvent e) {

			String tmp = protofile;
			if (tmp != null & tmp.length() > 0) {
				new SimpleEditor(tmp, false);
			} else {
				JOptionPane.showMessageDialog(frame,
				                              "ProMC.proto not attached", "Error",
				                              JOptionPane.ERROR_MESSAGE);
			}


		}
	}

	/**
	 * Show ProMC.proto
	 * 
	 * @author sergei
	 * 
	 */
	private class ShowStatProtoAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ShowStatProtoAction() {
			super("Statistics Layout");
		}

		public void actionPerformed(ActionEvent e) {


		}
	}




	/**
	 * Show ProMC.proto
	 * 
	 * @author sergei
	 * 
	 */
	private class ShowLogFileAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		ShowLogFileAction() {
			super("Log File");
		}

		public void actionPerformed(ActionEvent e) {

			if (file == null)
				return;

			if (logfile != null & logfile.length() > 0) {
				new SimpleEditor(logfile, true);
			} else {
				JOptionPane.showMessageDialog(frame,
				                              "Log file \"logfile.txt\" not attached", "Error",
				                              JOptionPane.ERROR_MESSAGE);
			}
		}

	}

}
