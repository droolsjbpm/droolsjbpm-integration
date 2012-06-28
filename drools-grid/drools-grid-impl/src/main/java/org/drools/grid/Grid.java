package org.drools.grid;

public interface Grid {

    public <T> T get(Class<T> serviceClass);

    public GridNode createGridNode( String id );
    
    public GridNode claimGridNode( String id );
    
    public void removeGridNode( String id );

    public GridNode getGridNode( String id );
    
    public String getId();

    public void dispose();

}
