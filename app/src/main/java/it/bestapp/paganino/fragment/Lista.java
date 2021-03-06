package it.bestapp.paganino.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;


//import it.bestapp.paganino.view.swiperefresh.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.bestapp.paganino.Charts;
import it.bestapp.paganino.Main;
import it.bestapp.paganino.R;
import it.bestapp.paganino.dialog.Login;
import it.bestapp.paganino.adapter.bustapaga.Busta;
import it.bestapp.paganino.adapter.bustapaga.BustaAdapter;
import it.bestapp.paganino.utility.connessione.HRConnect;
import it.bestapp.paganino.utility.connessione.PageDownloadedInterface;
import it.bestapp.paganino.utility.db.bin.BustaPaga;
import it.bestapp.paganino.utility.thread.ThreadHome;
import it.bestapp.paganino.utility.thread.ThreadDataPrepare;
import it.bestapp.paganino.utility.thread.ThreadStoreController;
import it.bestapp.paganino.view.swipelist.BaseSwipeListViewListener;
import it.bestapp.paganino.view.swipelist.SwipeListView;
import com.gc.materialdesign.widgets.ProgressDialog;
import com.gc.materialdesign.widgets.SnackBar;


public class Lista extends Fragment
        implements PageDownloadedInterface,
        SwipeRefreshLayout.OnRefreshListener,
        SearchView.OnQueryTextListener {

    private Activity act;
    private SwipeRefreshLayout swpRefresh;
    private ProgressDialog pDialog;
    private BustaAdapter adapter;
    private SwipeListView swpLstView;
    private HRConnect conn = null;
    private Login lDialog;
    private Lista _this;

    public BustaAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        act = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fra_lista, container, false);
        setHasOptionsMenu(true);

        conn = new HRConnect();
        lDialog = new Login(this,conn);
        adapter = new BustaAdapter(this, conn);
        _this   = this;

        dataPrepare();
        swpLstView = (SwipeListView) rootView.findViewById(R.id.swipList);
        swpRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_swipe);
        swpLstView.addSwipeRefresh(swpRefresh);

        swpLstView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            swpLstView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                    if (swpLstView.getPositionsSelected().size() > 0)
                        mode.setTitle("Confronta");
                    else
                        mode.finish();
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_confronta:
                            List<Integer> checked = swpLstView.getPositionsSelected();
                            if (checked.size() != 2) {
                                return true;
                            }
                            List<Busta> buste = new ArrayList<Busta>();
                            for (Integer i : checked) {
                                buste.add(adapter.getItem(i));
                            }
                            (new ThreadStoreController((Main) _this.getActivity(), buste, conn, 'C')).execute();

                            swpLstView.clearChoices();
                            ((BustaAdapter) swpLstView.getAdapter()).notifyDataSetChanged();

                            mode.finish();
                            return false;
                        default:
                            return false;
                    }
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.swipe, menu);
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    swpLstView.unselectedChoiceStates();
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }
            });
        }
        swpLstView.setSwipeListViewListener(new BaseSwipeListViewListener(this, swpLstView,conn,adapter ));
        swpLstView.setAdapter(adapter);

        swpRefresh.setOnRefreshListener(this);
        setup(swpLstView);
        return rootView;
    }


    private void dataPrepare() {
        try {
            (new ThreadDataPrepare(this, conn)).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setup(SwipeListView swpLst) {
        swpLstView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH); // there are five swiping modes
        swpLstView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL); //there are four swipe actions
        swpLstView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_REVEAL);
        swpLstView.setAnimationTime(50);
        swpLstView.setSwipeOpenOnLongPress(false);
    }



    @Override
    public void onPDFDownloaded(Busta bP, File f, char mode) {
        swpLstView.closeOpenedItems();
        Intent intent;
        switch (mode) {
            case 'S':
            //scarica popup operazione conclusa
                break;
            case 'V':   //visualizzo
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(f), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case 'D':   //drive
                ((Main) act).pushFile(f, bP);
                break;
            case 'E':   //excel
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(f), "application/vnd.ms-excel");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    startActivity(intent);
                }
                catch (ActivityNotFoundException e) {
                    Toast.makeText(_this.getActivity(), "No Application Available to View Excel", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onPDFDownloaded(BustaPaga b, char mode) {
        swpLstView.closeOpenedItems();
        Intent intent;
        switch (mode) {
            case 'G':   //grafico
                intent = new Intent( act, Charts.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                Bundle extras = new Bundle();
                extras.putParcelable("BUSTA", b);
                intent.putExtras(extras);

                startActivity(intent);
                break;
        }
    }



    @Override
    public void onResume(){
        super.onResume();
        adapter.updateReceiptsList();
    }

    @Override
    public void onStoreCompleted(List<Busta> buste) {

    }

    @Override
    public void onListaDownloaded(ArrayList<Busta> internet) {
        boolean trovato;
        for (Busta element : internet) {
            trovato = false;
            for (Busta localElement : adapter.getList()) {
                if (localElement.getID().equalsIgnoreCase(element.getID())) {
                    trovato = true;
                }
            }
            if (!trovato) {
                adapter.add(element);
            }
        }
        List<Busta> lista=adapter.getList();

        Collections.sort(lista);
        adapter.notifyDataSetChanged();
        swpRefresh.setRefreshing(false);
    }

    @Override
    public void procesDialog(boolean b) {
        if (b) {
            pDialog = new ProgressDialog(getActivity(), "Loading");
            pDialog.show();
        } else {
            pDialog.hide();
            pDialog.dismiss();
        }
    }

    @Override
    public void login() {
        lDialog.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        SearchManager searchManager = (SearchManager) act.getSystemService(Context.SEARCH_SERVICE);
        SearchView mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));

        if (mSearchView != null ) {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(act.getComponentName()));
            mSearchView.setIconifiedByDefault(false);
            mSearchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        swpLstView.setTextFilterEnabled(true);
        if (TextUtils.isEmpty(s)) {
            swpLstView.clearTextFilter();
        } else {
            swpLstView.setFilterText(s.toString());
        }
        return false;
    }

    @Override
    public void onRefresh() {
        if(isOnline(getActivity())){
            (new ThreadHome(this, conn)).execute();
        }else{
            swpRefresh.setRefreshing(false);
            SnackBar snackbar = new SnackBar(getActivity(), "Connessione assente vuoi uscire da paganino", "YES", new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            } );
            snackbar.show();
        }
    }
    public void stopRefresh() {
        swpRefresh.setRefreshing(false);
    }

    public void setConn(HRConnect c) {
        this.conn = c;
    }

    public static boolean isOnline (Activity act) {
        ConnectivityManager cm = ( ConnectivityManager ) act.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if(ni == null)
            return false;
        return ni.isConnected();
    }

}
