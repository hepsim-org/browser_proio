/* ---------------------------------------------------------------------------
 ** This software is in the public domain, furnished "as is", without technical
 ** support, and with no warranty, express or implied, as to its usefulness for
 ** any purpose.
 **
 **  A library for ProIO HEP events storage and processing based on Google's ProtocolBuffers 
 **
 ** Author: S.Chekanov (ANL). chekanov@anl.gov
 ** Copyright  2018
 ** -------------------------------------------------------------------------*/


package hepsimproio;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.text.*;
import proio.Reader;
import proio.Event;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

/**

Validate a ProIO file.
It validates the file and print separate events when requested.

@author S.Chekanov (ANL), J.Alcaraz (NIU). 

**/



final class Particle{

	public int pid,status;
	public double px,py,pz,e,m,x,y,z,t;

	public Particle(int pid,int status,double px,double py, double pz, double e, double m){
		this.pid=pid; this.status=status;
		this.px=px; this.py=py; this.pz=pz; this.e=e; this.m=m;
	}

	public void setXYZT(double x,double y, double z, double t){
		this.x=x; this.y=y; this.z=z; this.t=t;
	}

	public void print(){
		System.out.println("PID="+pid);
	};

};




public class Info {


	private static float unit=0;
	private static float lunit=0;
	private static String ptag="Particle";


	public static void main(String[] args) {



		int nevent=-1;
		if (args.length == 2) {
			System.out.println("HepSim: Look at event "+args[1].trim());
			try  {
				nevent = Integer.parseInt(args[1]);

				try {
					info_seeker(args[0]);
					event_seeker(args[0],nevent);
				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (NumberFormatException e) {
				HepSim.ErrorMessage("Error: Cannot parse event number. It is not integer number! Exit!");
				System.exit(1);
			}
		} else if (args.length == 1) {

			try {
				info_seeker(args[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}  else {
			HepSim.ErrorMessage("Usage: It takes 1 or 2 arguments:  ProIO file location (or its URL) and event to look at (optional)");
			System.exit(1);
		}
	}





	// look for Metadata
	private static String info_seeker(String args) throws IOException {

		System.out.println("File          = "+args);

		int nn=0;

		try {
			FileMC fm= new FileMC(args);
			Reader reader = fm.reader();
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

			String  eCM="";
			String  idB="";
			String  idA="";
			String requested="0";
			String logfile="";
			String lumi="0";
			String events="0";
			String description="none";
			String time="none";
			String cross="0";
			String error="0";
			Map<String, ByteString> metadata = storeevent.getMetadata();
			for (Map.Entry<String, ByteString> entry : metadata.entrySet()) {

				String skey=entry.getKey();
				String name=entry.getValue().toStringUtf8();
				if (!name.equals("logfile")) {
					//System.out.println(skey);
					if (skey.equals("meta:cross_section_pb")) cross=name;
					else if (skey.equals("meta:cross_section_pb_err")) error=name;
					else if (skey.equals("meta:creation_time")) time=name.replace("\n","");
					else if (skey.equals("meta:description")) description=name.replace("\n","");
					else if (skey.equals("meta:events")) events=name;
					else if (skey.equals("meta:luminosity_inv_pb")) lumi=name;
					else if (skey.equals("meta:logfile")) logfile=name;
					else if (skey.equals("meta:events_requested")) requested=name;
					else if (skey.equals("info:eCM")) eCM=name;
					else if (skey.equals("info:idB")) idB=name;
					else if (skey.equals("info:idA")) idA=name;
					else if (skey.equals("info:varint_energy")) {
						unit = (float)Double.parseDouble(name);
					} else if (skey.equals("info:varint_length")) {
						lunit = (float)Double.parseDouble(name);
					}

					//System.out.println("Metadata: " + entry.getKey() + ": " + entry.getValue().toStringUtf8());
				}
			}

			System.out.println("Last modified = "+time);
			System.out.println("Description   = "+description);
			System.out.println("Events        = "+events);
			System.out.println("Requested     = "+requested);
			System.out.println("Sigma    (pb) = "+cross+" \u00B1 "+error );
			System.out.println("Lumi   (pb-1) = "+lumi);
			System.out.println("CME           = "+eCM + " GeV,  ID1 = "+idA+", ID2 = "+idB);
			if (unit>0)  System.out.println("Varint unit for energy = "+Double.toString(unit));
			if (lunit>0) System.out.println("Varint unit for length = "+Double.toString(unit));

			boolean isLog=false;
			if (logfile.length()>1) isLog=true;
			if (isLog==false)
				System.err.println(HepSim.ANSI_YELLOW+"Warning: Log file \"logfile.txt\" was not attached!"+HepSim.ANSI_RESET);
			else System.out.println("Log file:     = logfile"  );

			if (nn-1 == Integer.valueOf(events))
				System.out.println(HepSim.ANSI_GREEN+"####  The file is healthy!  ####"+HepSim.ANSI_RESET );
			else
				System.out.println(HepSim.ANSI_RED+"####  Error: File is corrupted. Inconsistent number of events!  ####"+HepSim.ANSI_RESET );


		} catch (Throwable e) {
			System.out.println(e);
		}
		return "";
	}


	// get ID
	private static int getID(Object obj) {
		int barcode=0;
		if (obj instanceof proio.model.Mc.Particle) {
			proio.model.Mc.Particle par = (proio.model.Mc.Particle) obj;
			barcode=par.getId();
		};
		return barcode;

	}


	// look at event
	private static String event_seeker(String args, int nrevent) throws IOException {

		DecimalFormat formatter = new DecimalFormat("0.#####E0");
		DecimalFormat formatter1 = new DecimalFormat("0.###E0");
		Map<Integer, String> name_map = hepsimproio.HepSim.GetPDGNames();

		System.out.println("File          = "+args);


		try {
			FileMC fm= new FileMC(args);
			Reader reader = fm.reader();
			try {

				//System.out.println("Read");
				//reader.skip((long)nrevent);
				//Event event = reader.next(true);

				reader.skip((long)nrevent-1);


				long ntot=0;
				double ene=0;

				String ID="%5s";
				String L="%10s";
				String E="%10s";
				int nn=0;

				for (Event event : reader) {

					System.out.println("## tag: MCParameters ##");
					for (long entryID : event.getTaggedEntries("MCParameters")) {
						System.out.println(event.getEntry(entryID).toString());
					}

					System.out.println("## tag: "+ptag+" ##");
					System.out.println("Units: Energy in GeV,  Length in mm,  Time in mm/c");
					System.out.format("%6s%12s%6s"+ID+ID+ID+ID+ID+E+E+E+E+E+L+L+L+L+"\n","Nr","Name","PID","Stat","M1","M2","D1","D2","Px","Py","Pz","E","Mass","X","Y","Z","T");

					int nlinse=6+10+6+5*(5)+5*(10)+4*(10);
					for (int j=0; j<nlinse; j++) System.out.print("-");
					System.out.print("\n");








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
							String mm="Found VarintPackedParticles, but varint units \"info:varint_energy\" and \"info:varint_length\" are not set!. Exit";
							System.out.println(HepSim.ANSI_RED+"####  Error: "+mm+" ####"+HepSim.ANSI_RESET );
							System.exit(0);
						}

						ntot=0;
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

							String sid=Long.toString(id);
							String spid=Integer.toString(pid);
							String sstatus=Integer.toString(status);
							String sm1=Integer.toString(m1);
							String sm2=Integer.toString(m2);
							String sd1=Integer.toString(d1);
							String sd2=Integer.toString(d2);
							String spx=formatter1.format(px);
							String spy=formatter1.format(py);
							String spz=formatter1.format(pz);
							String see=formatter1.format(ee);
							String sm=formatter1.format(m);
							String sx=formatter1.format(x);
							String sy=formatter1.format(y);
							String sz=formatter1.format(z);
							String st=formatter1.format(t);

							System.out.format("%6s%12s%6s"+ID+ID+ID+ID+ID+E+E+E+E+E+L+L+L+L+"\n",sid,name,spid,sstatus,sm1,sm2,sd1,sd2,spx,spy,spz,see,sm,sx,sy,sz,st);

							ntot++;
							ene=ene+ee;

						}

					}; // end of varintPacketparticles




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
						ntot=0;
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

							String sid=Long.toString(id);
							String spid=Integer.toString(pid);
							String sstatus=Integer.toString(status);
							String sm1=Integer.toString(m1);
							String sm2=Integer.toString(m2);
							String sd1=Integer.toString(d1);
							String sd2=Integer.toString(d2);
							String spx=formatter1.format(px);
							String spy=formatter1.format(py);
							String spz=formatter1.format(pz);
							String see=formatter1.format(ee);
							String sm=formatter1.format(m);
							String sx=formatter1.format(x);
							String sy=formatter1.format(y);
							String sz=formatter1.format(z);
							String st=formatter1.format(t);

							System.out.format("%6s%12s%6s"+ID+ID+ID+ID+ID+E+E+E+E+E+L+L+L+L+"\n",sid,name,spid,sstatus,sm1,sm2,sd1,sd2,spx,spy,spz,see,sm,sx,sy,sz,st);

							ntot++;
							ene=ene+ee;


						}

					}; // end of Packetparticles








					ntot=0;
					for (long entryID : event.getTaggedEntries("Particle")) {
						ptag="Particle";
						Object obj = event.getEntry(entryID);
						if (obj instanceof proio.model.Mc.Particle) {
							proio.model.Mc.Particle par = (proio.model.Mc.Particle) obj;


							proio.model.Mc.XYZTF vertex=par.getVertex();
							proio.model.Mc.XYZF p=par.getP();

							int id = par.getId();
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

							String sid=Long.toString(id);
							String spid=Integer.toString(pid);
							String sstatus=Integer.toString(status);
							String sm1=Integer.toString(m1);
							String sm2=Integer.toString(m2);
							String sd1=Integer.toString(d1);
							String sd2=Integer.toString(d2);
							String spx=formatter1.format(px);
							String spy=formatter1.format(py);
							String spz=formatter1.format(pz);
							String see=formatter1.format(ee);
							String sm=formatter1.format(m);
							String sx=formatter1.format(x);
							String sy=formatter1.format(y);
							String sz=formatter1.format(z);
							String st=formatter1.format(t);

							String name = "none";
							if (name_map.containsKey(pid))
								name = name_map.get(pid);
							name =  name.substring(0, Math.min(name.length(), 11));

							System.out.format("%6s%12s%6s"+ID+ID+ID+ID+ID+E+E+E+E+E+L+L+L+L+"\n",sid,name,spid,sstatus,sm1,sm2,sd1,sd2,spx,spy,spz,see,sm,sx,sy,sz,st);

							ntot++;
							ene=ene+ee;



						} // loop over particle


						// model independent method
						// System.out.println(event.getEntry(entryID).toString());
					}

					// post line
					for (int j=0; j<nlinse; j++) System.out.print("-");
					System.out.print("\n");
					System.out.println("Nr of stored particles="+Long.toString(ntot));
					System.out.println("Event energy    (GeV) ="+formatter.format(ene));


					break;
				}; // end event





			} catch (Throwable e) {
				;
			}

		} catch (Throwable e) {
			System.out.println(e);
		}
		return "";
	}




	// Performs heavy lifting for collection entry introspection
	private static Particle getParticle(Message msg) {
		String returnString = "";

		Descriptors.Descriptor desc = msg.getDescriptorForType();
		List<Descriptors.FieldDescriptor> fields = desc.getFields();

		int pid=0;
		double px=0;
		double py=0;
		double pz=0;
		double e=0;
		double m=0;
		double x=0;
		double y=0;
		double z=0;
		double t=0;
		int status=0;
		for (Descriptors.FieldDescriptor field : fields) {
			if (!field.isRepeated()) {
				if (msg.hasField(field)) {
					//returnString = returnString + "\n" + field.getName() + ": ";
					Object value = msg.getField(field);
					//String fvalue=getFieldValueString(field, value);
					//returnString = returnString + getFieldValueString(field, value);
					System.out.println(field.getName());
					if (field.getName().equals("pdg")) pid=(int)value; // sid=getFieldValueString(field, value);
					if (field.getName().equals("status")) status=(int)value; // sid=getFieldValueString(field, value);
					if (field.getName().equals("energy")) e=(float)value; // sid=getFieldValueString(field, value);
		
				}
			} else {
				int count = msg.getRepeatedFieldCount(field);

				for (int i = 0; i < count; i++) {
					//returnString = returnString + "\n" + field.getName() + "[" + Integer.toString(i) + "]: ";
					Object value = msg.getRepeatedField(field, i);
					String fieldString = getFieldValueString(field, value);
					//returnString = returnString + fieldString;
				}

			}
		}

		Particle p = new Particle(pid,status,px,py,pz,e,m);
		p.print();

		return p;
	}




	// Performs heavy lifting for collection entry introspection
	private static String getMessageString(Message msg) {
		String returnString = "";

		Descriptors.Descriptor desc = msg.getDescriptorForType();
		List<Descriptors.FieldDescriptor> fields = desc.getFields();

		for (Descriptors.FieldDescriptor field : fields) {
			if (!field.isRepeated()) {
				if (msg.hasField(field)) {
					returnString = returnString + "\n" + field.getName() + ": ";
					Object value = msg.getField(field);
					returnString = returnString + getFieldValueString(field, value);
				}
			} else {
				int count = msg.getRepeatedFieldCount(field);
				for (int i = 0; i < count; i++) {
					returnString = returnString + "\n" + field.getName() + "[" + Integer.toString(i) + "]: ";
					Object value = msg.getRepeatedField(field, i);
					String fieldString = getFieldValueString(field, value);
					returnString = returnString + fieldString;
				}
			}
		}

		return returnString;
	}



	// Performs heavy lifting for collection entry introspection
	private static String getFieldValueString(Descriptors.FieldDescriptor field, Object value) {
		String returnString = "";

		switch (field.getType()) {
		case INT32:
		case UINT32:
			returnString = Integer.toString((Integer) value);
			break;
		case SINT32:
			returnString = Integer.toString((Integer) value);
			break;
		case INT64:
		case UINT64:
			returnString = Long.toString((Long) value);
			break;
		case FLOAT:
			returnString = Float.toString((Float) value);
			break;
		case DOUBLE:
			returnString = Double.toString((Double) value);
			break;
		case STRING:
			returnString = (String) value;
			break;
		case MESSAGE:
			returnString = getMessageString((Message) value).replaceAll("\n", "\n\t");
			break;
		}

		return returnString;
	}




















}





