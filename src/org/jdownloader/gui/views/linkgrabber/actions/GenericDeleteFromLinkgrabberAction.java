package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class GenericDeleteFromLinkgrabberAction extends AppAction implements CachableInterface {
    public static final String ONLY_SELECTED_ITEMS = "onlySelectedItems";
    /**
     * 
     */
    private static final long  serialVersionUID    = 1L;
    public static final String DELETE_ALL          = "deleteAll";
    public static final String DELETE_DISABLED     = "deleteDisabled";
    public static final String DELETE_OFFLINE      = "deleteOffline";
    private List<AbstractNode> currentSelection;

    private boolean            deleteDisabled      = false;
    private boolean            onlySelectedItems   = false;

    private boolean            deleteAll           = false;

    private boolean            ignoreFiltered      = true;

    private boolean            deleteOffline       = false;

    private List<CrawledLink>  filteredCrawledLinks;

    public GenericDeleteFromLinkgrabberAction() {
        super();
        this.setIconKey(IconKey.ICON_DELETE);

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        this.update();

        final List<CrawledLink> lfilteredCrawledLinks = this.filteredCrawledLinks;

        if (lfilteredCrawledLinks != null && lfilteredCrawledLinks.size() > 0) {
            try {
                boolean containsOnline = false;
                for (CrawledLink cl : lfilteredCrawledLinks) {
                    if (TYPE.OFFLINE == cl.getParentNode().getType()) continue;
                    if (TYPE.POFFLINE == cl.getParentNode().getType()) continue;
                    if (cl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                        containsOnline = true;
                        break;
                    }

                }

                if (containsOnline) {
                    // only ask for online links
                    Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_are_you_sure(), _GUI._.GenericDeleteFromDownloadlistAction_actionPerformed_ask_(this.createName()), null, _GUI._.literally_yes(), _GUI._.literall_no());
                }
                LinkCollector.getInstance().removeChildren(lfilteredCrawledLinks);

            } catch (DialogNoAnswerException e1) {
            }
            return;
        }
        Toolkit.getDefaultToolkit().beep();
        Dialog.getInstance().showErrorDialog(_GUI._.GenericDeleteSelectedToolbarAction_actionPerformed_nothing_to_delete_());
    }

    private LinkGrabberTable getTable() {
        return (LinkGrabberTable) LinkGrabberTableModel.getInstance().getTable();
    }

    @Customizer(name = "Include All Links")
    public boolean isDeleteAll() {
        return this.deleteAll;
    }

    @Customizer(name = "Include disabled Links")
    public boolean isDeleteDisabled() {
        return this.deleteDisabled;
    }

    @Customizer(name = "Include Offline Links")
    public boolean isDeleteOffline() {
        return this.deleteOffline;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Customizer(name = "Exclude filtered Links")
    public boolean isIgnoreFiltered() {
        return this.ignoreFiltered;
    }

    @Customizer(name = "Only Selected Links")
    public boolean isOnlySelectedItems() {

        return this.onlySelectedItems;
    }

    @Override
    public void setData(final String data) {
    }

    public void setDeleteAll(final boolean deleteIdle) {
        this.deleteAll = deleteIdle;
        this.updateName();
    }

    public void setDeleteDisabled(final boolean deleteDisabled) {
        this.deleteDisabled = deleteDisabled;
        this.updateName();
    }

    public void setDeleteOffline(final boolean deleteOffline) {
        this.deleteOffline = deleteOffline;
        this.updateName();
    }

    public void setIgnoreFiltered(final boolean ignoreFiltered) {
        this.ignoreFiltered = ignoreFiltered;
    }

    public void setOnlySelectedItems(final boolean onlySelectedItems) {
        this.onlySelectedItems = onlySelectedItems;
    }

    private void update() {

        if (this.isOnlySelectedItems()) {
            this.currentSelection = getTable().getModel().getSelectedObjects();
        } else {
            if (this.isIgnoreFiltered()) {
                this.currentSelection = getTable().getModel().getElements();
            } else {

                this.currentSelection = new ArrayList<AbstractNode>(getTable().getModel().getController().getAllChildren());
            }

        }
        this.filteredCrawledLinks = null;

        if (this.currentSelection != null && this.currentSelection.size() > 0) {

            final SelectionInfo<CrawledPackage, CrawledLink> si = new SelectionInfo<CrawledPackage, CrawledLink>(null, this.currentSelection, null, null, null, getTable());

            List<CrawledLink> filtered = new ArrayList<CrawledLink>();
            for (CrawledLink cl : si.getChildren()) {
                if (isDeleteAll()) {
                    filtered.add(cl);
                    continue;
                }
                if (isDeleteDisabled() && !cl.isEnabled()) {
                    filtered.add(cl);
                    continue;
                }

                if (isDeleteOffline() && cl.getDownloadLink().isAvailabilityStatusChecked() && cl.getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE) {
                    filtered.add(cl);
                    continue;
                }
            }
            filteredCrawledLinks = filtered;

        }

    }

    private void updateName() {

        // if (isDeleteFailed() && isDeleteDisabled() && isDeleteFinished() && isDeleteOffline()) {
        // setName(_GUI._.ContextMenuFactory_createPopup_cleanup_only());
        // } else {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(createName());
            }
        };

        // }

    }

    private String createName() {
        final StringBuilder sb = new StringBuilder();

        if (this.isDeleteAll()) {
            if (this.isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected_all());
            } else {
                sb.append(_GUI._.GenericDeleteFromLinkgrabberAction_createName_updateName_object_all());
            }
        } else {
            if (this.isOnlySelectedItems()) {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_selected());
            } else {
                sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object());
            }
            boolean first = true;

            if (this.isDeleteDisabled()) {
                if (!first) {
                    sb.append(" & ");
                }
                sb.append(_GUI._.lit_disabled());
                first = false;
            }

            if (this.isDeleteOffline()) {
                if (!first) {
                    sb.append(" & ");
                }
                first = false;
                sb.append(_GUI._.lit_offline());
            }
        }
        return sb.toString();
    }

}