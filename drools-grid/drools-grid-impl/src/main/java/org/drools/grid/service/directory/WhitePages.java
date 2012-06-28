package org.drools.grid.service.directory;

import org.drools.grid.GridServiceDescription;

public interface WhitePages {

    public GridServiceDescription create( String serviceDescriptionId, String ownerGridId );

    public void remove( String serviceDescriptionId );

    public GridServiceDescription lookup( String serviceDescriptionId );

}
