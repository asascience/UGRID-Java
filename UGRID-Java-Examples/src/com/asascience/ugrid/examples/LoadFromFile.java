package com.asascience.ugrid.examples;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.UGridDataset;
import ucar.nc2.dt.ugrid.Cell;
import ucar.nc2.dt.ugrid.Entity;
import ucar.nc2.dt.ugrid.Mesh;
import ucar.nc2.dt.UGridDataset.Meshset;
import ucar.nc2.dt.UGridDatatype;
import ucar.nc2.dt.ugrid.MeshVariable;
import ucar.nc2.dt.ugrid.geom.LatLonPoint2D;
import ucar.nc2.dt.ugrid.geom.LatLonRectangle2D;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

/**
 *
 * @author Kyle
 */
public class LoadFromFile {

  public static void main(String[] args) throws IOException {
    testUGRID();
  }

  private static void testUGRID() {
    CancelTask cancelTask = null;
    //String unstructured = "C:/Dev/Unstructured/ugrid/FVCOM/fvcom_delt.ncml";
    String unstructured = "C:/Dev/Unstructured/ugrid/ELCIRC/elcirc_delt.ncml";
    //String unstructured = "C:/Dev/Unstructured/ugrid/ADCIRC/adcirc_delt.ncml";
    //String unstructured = "C:/Dev/Unstructured/ugrid/SELFE/selfe_delt.ncml";
    //String unstructured = "dods://testbedapps.sura.org/thredds/dodsC/ugrid/TestCases/ELCIRC/elcirc_delt.ncml";
    try {
      UGridDataset ugrid = (UGridDataset) FeatureDatasetFactoryManager.open(FeatureType.UGRID, unstructured, cancelTask, new Formatter());
      long startTime;
      long endTime;
      for (Meshset ms : ugrid.getMeshsets()) {
        Mesh m = ms.getMesh();
        System.out.println(m.toString());
        // We build now, to see how grids compare in index timeUGridDataset ugrid = (UGridDataset) FeatureDatasetFactoryManager.open(FeatureType.UGRID, unstructured, cancelTask, new Formatter());
        System.out.println("Building RTree...");
        startTime = System.currentTimeMillis();
        m.buildRTree();
        endTime = System.currentTimeMillis();
        System.out.println("RTree build took: " + (double) (endTime - startTime) / 1000 + " seconds.");
        System.out.println("RTree contains: " + m.getTreeSize() + " entries.");

        if (m.getTreeSize() > 0) {
          // Query a random point within the bounding box of the Mesh
          LatLonRect bounds = m.getLatLonBoundingBox();
          double query_lat = bounds.getLatMin() + (Math.random() * (bounds.getLatMax() - bounds.getLatMin()));
          double query_lon = bounds.getLonMin() + (Math.random() * (bounds.getLonMax() - bounds.getLonMin()));
          LatLonPoint query_point = new LatLonPointImpl(query_lat, query_lon);
          System.out.println("Random query point: " + query_lat + "," + query_lon);

          // Get the Cell that the point is in
          Cell cell = m.getCellFromLatLon(query_lat, query_lon);
          System.out.println("Cell containing point located.");

          List<LatLonPoint2D> vertices = cell.getPolygon().getVertices();
          System.out.println("Cell vertices (" + vertices.size() + "):");
          for (LatLonPoint2D p : vertices) {
            System.out.println(p.getLatitude() + "," + p.getLongitude());
          }
          System.out.println("Cell center: " + cell.getPolygon().getCentroid().getLatitude() + "," + cell.getPolygon().getCentroid().getLongitude());

          // The Cell contains many Entities, which are points which data
          // can lay on.  Can test if the Entity is on the boundry.
          System.out.println("Data locations within the Cell:");
          for (Entity e : cell.getEntities()) {
            System.out.println(e.getClass().getSimpleName() + ": " + e.getGeoPoint().getLatitude() + "," + e.getGeoPoint().getLongitude());
          }

          for (UGridDatatype v : ms.getMeshVariables()) {
            v = (MeshVariable) v;
            // Extract value for each variable at the query point
            System.out.print(v.getName() + ": ");
            System.out.print(v.readPointData(query_point));
            System.out.println();
            //System.out.println(m.extractDataPoints(v).size());

          }
        }

        // Subset
        if (m.getSize() > 0) {
          /*
           * This is what we are subsetting
           * 
          ur--------------
          |xxxxxx|      |
          |xxxxxx|      |
          ---------------
          |      |      |
          |      |      |
          --------------lr
           */
          LatLonRect bounds = m.getLatLonBoundingBox();
          LatLonRectangle2D rect = new LatLonRectangle2D(bounds.getUpperLeftPoint().getLatitude(), bounds.getUpperLeftPoint().getLongitude(), bounds.getLowerRightPoint().getLatitude(), bounds.getLowerRightPoint().getLongitude());
          LatLonRectangle2D rect2 = new LatLonRectangle2D(new LatLonPoint2D.Double(rect.getUpperLeftPoint().getLatitude(), rect.getUpperLeftPoint().getLongitude()), new LatLonPoint2D.Double(rect.getCenterLatitude(), rect.getCenterLongitude()));
          LatLonRect h = rect2.toLatLonRect();
          System.out.println("Subsetting the top left corner of bounding box...");
          UGridDataset ug2 = ((MeshVariable) ms.getMeshVariableByName("zeta")).subset(h);
          for (Meshset ms2 : ug2.getMeshsets()) {
            Mesh m2 = ms2.getMesh();
            m2.buildRTree();
            System.out.println(m2.toString());
          }
        }
      }
    } catch (IOException e) {
      System.out.println(e);
    }
  }
}
