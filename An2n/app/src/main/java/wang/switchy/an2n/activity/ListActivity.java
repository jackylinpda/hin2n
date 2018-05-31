package wang.switchy.an2n.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import wang.switchy.an2n.An2nApplication;
import wang.switchy.an2n.R;
import wang.switchy.an2n.adapter.SettingItemAdapter;
import wang.switchy.an2n.entity.SettingItemEntity;
import wang.switchy.an2n.event.ErrorEvent;
import wang.switchy.an2n.event.StartEvent;
import wang.switchy.an2n.event.StopEvent;
import wang.switchy.an2n.model.N2NSettingInfo;
import wang.switchy.an2n.service.N2NService;
import wang.switchy.an2n.storage.db.base.N2NSettingModelDao;
import wang.switchy.an2n.storage.db.base.model.N2NSettingModel;
import wang.switchy.an2n.template.BaseTemplate;
import wang.switchy.an2n.template.CommonTitleTemplate;
import wang.switchy.an2n.tool.N2nTools;


/**
 * Created by janiszhang on 2018/5/4.
 */

public class ListActivity extends BaseActivity {

    private SwipeMenuListView mSettingsListView;
    private SettingItemAdapter mSettingItemAdapter;
    private ArrayList<SettingItemEntity> mSettingItemEntities;

    private SharedPreferences mAn2nSp;
    private SharedPreferences.Editor mAn2nEdit;
    private N2NSettingModel mN2NSettingModel;
    private SweetAlertDialog mSweetAlertDialog;
    private int mTargetSettingPosition;

    @Override
    protected BaseTemplate createTemplate() {
        CommonTitleTemplate titleTemplate = new CommonTitleTemplate(mContext, "Setting List");
        titleTemplate.mRightImg.setVisibility(View.VISIBLE);
        titleTemplate.mRightImg.setImageResource(R.mipmap.ic_add);
        titleTemplate.mRightImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ListActivity.this, SettingDetailsActivity.class);
                intent.putExtra("type", SettingDetailsActivity.TYPE_SETTING_ADD);
                startActivity(intent);
            }
        });

        titleTemplate.mLeftImg.setVisibility(View.VISIBLE);
        titleTemplate.mLeftImg.setImageResource(R.drawable.titlebar_icon_return_selector);
        titleTemplate.mLeftImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                // TODO: 2018/5/4
            }
        });

        return titleTemplate;
    }

    @Override
    protected void doOnCreate(Bundle savedInstanceState) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        mAn2nSp = getSharedPreferences("An2n", MODE_PRIVATE);
        mAn2nEdit = mAn2nSp.edit();

        mSettingsListView = (SwipeMenuListView) findViewById(R.id.lv_setting_item);

        mSettingItemEntities = new ArrayList<>();

        mSettingItemAdapter = new SettingItemAdapter(this, mSettingItemEntities);

        mSettingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {

                Log.e("0531", "mSettingsListView setOnItemClickListener = " + position);

                final Long currentSettingId = mAn2nSp.getLong("current_setting_id", -1);

                SettingItemEntity settingItemEntity = mSettingItemEntities.get(position);
                Long saveId = settingItemEntity.getSaveId();

                if (currentSettingId.equals(saveId)) {
                    return;
                }

                if (N2NService.INSTANCE != null && N2NService.INSTANCE.getEdgeStatus().isRunning) {
                    mSweetAlertDialog = new SweetAlertDialog(ListActivity.this, SweetAlertDialog.WARNING_TYPE);
                    mSweetAlertDialog
                            .setTitleText("Change the setting ?")
//                                .setContentText("Won't be able to recover this file!")
                            .setCancelText("No")
                            .setConfirmText("Yes")
                            .showCancelButton(true)
                            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    sDialog.cancel();
                                }
                            })
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    N2NService.INSTANCE.stop();

                                    mTargetSettingPosition = position;

                                    Intent vpnPrepareIntent = VpnService.prepare(ListActivity.this);

                                    if (vpnPrepareIntent != null) {
                                        startActivityForResult(vpnPrepareIntent, 100);
                                    } else {
                                        onActivityResult(100, -1, null);

                                    }

                                    if (currentSettingId != -1) {
                                        N2NSettingModel currentSettingItem = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao().load((long) currentSettingId);
                                        if (currentSettingItem != null) {
                                            currentSettingItem.setIsSelcected(false);
                                            An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao().update(currentSettingItem);
                                        }
                                    }

                                    for (int i = 0; i < mSettingItemEntities.size(); i++) {
                                        mSettingItemEntities.get(i).setSelected(false);
                                    }

                                    N2NSettingModelDao n2NSettingModelDao = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao();
                                    mN2NSettingModel = n2NSettingModelDao.load(mSettingItemEntities.get(position).getSaveId());
                                    mN2NSettingModel.setIsSelcected(true);

                                    n2NSettingModelDao.update(mN2NSettingModel);

                                    mAn2nEdit.putLong("current_setting_id", mN2NSettingModel.getId());
                                    mAn2nEdit.commit();
                                    mSettingItemEntities.get(position).setSelected(true);
                                    mSettingItemAdapter.notifyDataSetChanged();

                                    sweetAlertDialog.cancel();
                                }
                            })
                            .show();
                }else {
                    if (currentSettingId != -1) {
                        N2NSettingModel currentSettingItem = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao().load((long) currentSettingId);
                        if (currentSettingItem != null) {
                            currentSettingItem.setIsSelcected(false);
                            An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao().update(currentSettingItem);
                        }
                    }

                    for (int i = 0; i < mSettingItemEntities.size(); i++) {
                        mSettingItemEntities.get(i).setSelected(false);
                    }

                    N2NSettingModelDao n2NSettingModelDao = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao();
                    mN2NSettingModel = n2NSettingModelDao.load(mSettingItemEntities.get(position).getSaveId());
                    mN2NSettingModel.setIsSelcected(true);

                    n2NSettingModelDao.update(mN2NSettingModel);

                    mAn2nEdit.putLong("current_setting_id", mN2NSettingModel.getId());
                    mAn2nEdit.commit();
                    mSettingItemEntities.get(position).setSelected(true);
                    mSettingItemAdapter.notifyDataSetChanged();
                }

            }
        });

        /*****************侧滑菜单 begin********************/
        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                SwipeMenuItem copyItem = new SwipeMenuItem(getApplicationContext());
                copyItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                copyItem.setWidth(N2nTools.dp2px(ListActivity.this, 70));
                copyItem.setTitle("Copy");
                copyItem.setTitleSize(18);
                copyItem.setTitleColor(Color.WHITE);
                menu.addMenuItem(copyItem);

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(N2nTools.dp2px(ListActivity.this, 70));
                // set a icon
//                deleteItem.setIcon(R.mipmap.ic_launcher);
                deleteItem.setTitle("Delete");
                deleteItem.setTitleSize(18);
                deleteItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(deleteItem);

            }
        };

        // set creator
        mSettingsListView.setMenuCreator(creator);

        // Right
        mSettingsListView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

        mSettingsListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {


                final SettingItemEntity settingItemEntity = mSettingItemEntities.get(position);

                switch (index) {

                    case 0:
                        N2NSettingModelDao n2NSettingModelDao1 = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao();
                        N2NSettingModel n2NSettingModelCopy = n2NSettingModelDao1.load(settingItemEntity.getSaveId());

                        //1.db update
                        String copyName = n2NSettingModelCopy.getName() + "-copy";
                        String copyNameTmp = copyName;

                        int i = 0;
                        while (n2NSettingModelDao1.queryBuilder().where(N2NSettingModelDao.Properties.Name.eq(copyName)).unique() != null) {
                            i++;
                            copyName = copyNameTmp + "(" + i + ")";

                        }

                        N2NSettingModel n2NSettingModel = new N2NSettingModel(null, copyName, n2NSettingModelCopy.getIp(), n2NSettingModelCopy.getNetmask(), n2NSettingModelCopy.getCommunity(),
                                n2NSettingModelCopy.getPassword(), n2NSettingModelCopy.getSuperNode(), n2NSettingModelCopy.getMoreSettings(), n2NSettingModelCopy.getSuperNodeBackup(),
                                n2NSettingModelCopy.getMacAddr(), n2NSettingModelCopy.getMtu(), n2NSettingModelCopy.getLocalIP(), n2NSettingModelCopy.getHolePunchInterval(),
                                n2NSettingModelCopy.getResoveSupernodeIP(), n2NSettingModelCopy.getLocalPort(), n2NSettingModelCopy.getAllowRouting(), n2NSettingModelCopy.getDropMuticast(),
                                n2NSettingModelCopy.getTraceLevel(), false);

                        n2NSettingModelDao1.insert(n2NSettingModel);

                        //2.ui update

                        final SettingItemEntity settingItemEntity2 = new SettingItemEntity(n2NSettingModel.getName(),
                                n2NSettingModel.getId(), n2NSettingModel.getIsSelcected());

                        settingItemEntity2.setOnMoreBtnClickListener(new SettingItemEntity.OnMoreBtnClickListener() {
                            @Override
                            public void onClick(int positon) {
                                Intent intent = new Intent(ListActivity.this, SettingDetailsActivity.class);
                                intent.putExtra("type", SettingDetailsActivity.TYPE_SETTING_MODIFY);
                                intent.putExtra("saveId", settingItemEntity2.getSaveId());

                                startActivity(intent);
                            }
                        });
                        mSettingItemEntities.add(settingItemEntity2);

                        mSettingItemAdapter.notifyDataSetChanged();

                        break;

                    case 1:

                        final SettingItemEntity finalSettingItemEntity = settingItemEntity;
                        new SweetAlertDialog(ListActivity.this, SweetAlertDialog.WARNING_TYPE)
                                .setTitleText("Are you sure?")
                                .setCancelText("No,cancel plx!")
                                .setConfirmText("Yes,delete it!")
                                .showCancelButton(true)
                                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sDialog) {
                                        sDialog.cancel();
                                    }
                                })
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        N2NSettingModelDao n2NSettingModelDao = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao();
                                        n2NSettingModelDao.deleteByKey(finalSettingItemEntity.getSaveId());

                                        mSettingItemEntities.remove(finalSettingItemEntity);
                                        mSettingItemAdapter.notifyDataSetChanged();

                                        N2NService.INSTANCE.stop();

                                        sweetAlertDialog.cancel();
                                    }
                                })
                                .show();


                        break;
                    default:

                        break;
                }

                return false;
            }
        });

        /*****************侧滑菜单 end********************/

        mSettingsListView.setAdapter(mSettingItemAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        N2NSettingModelDao n2NSettingModelDao = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao();
        List<N2NSettingModel> n2NSettingModels = n2NSettingModelDao.loadAll();

        N2NSettingModel n2NSettingModel;
        //需要判空吗？
        mSettingItemEntities.clear();
        for (int i = 0; i < n2NSettingModels.size(); i++) {
            n2NSettingModel = n2NSettingModels.get(i);
            final SettingItemEntity settingItemEntity = new SettingItemEntity(n2NSettingModel.getName(),
                    n2NSettingModel.getId(), n2NSettingModel.getIsSelcected());

            settingItemEntity.setOnMoreBtnClickListener(new SettingItemEntity.OnMoreBtnClickListener() {

                @Override
                public void onClick(int positon) {

                    Intent intent = new Intent(ListActivity.this, SettingDetailsActivity.class);
                    intent.putExtra("type", SettingDetailsActivity.TYPE_SETTING_MODIFY);
                    intent.putExtra("saveId", settingItemEntity.getSaveId());

                    startActivity(intent);

                }
            });
            mSettingItemEntities.add(settingItemEntity);
        }

        mSettingItemAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("zhangbz", "onActivityResult requestCode = " + requestCode + "; resultCode = " + resultCode);
        if (requestCode == 100 && resultCode == -1) {//RESULT_OK

            Log.e("0531", "onActivityResult targetSettingPosition = " + mTargetSettingPosition);

            SettingItemEntity settingItemEntity = mSettingItemEntities.get(mTargetSettingPosition);

            N2NSettingModelDao n2NSettingModelDao1 = An2nApplication.getInstance().getDaoSession().getN2NSettingModelDao();
            N2NSettingModel n2NSettingModel = n2NSettingModelDao1.load(settingItemEntity.getSaveId());

            Intent intent = new Intent(ListActivity.this, N2NService.class);
            Bundle bundle = new Bundle();

            N2NSettingInfo n2NSettingInfo = new N2NSettingInfo(n2NSettingModel);
            bundle.putParcelable("n2nSettingInfo", n2NSettingInfo);
            intent.putExtra("Setting", bundle);

            startService(intent);
        }
    }

    @Override
    protected int getContentLayout() {
        return R.layout.activity_setting_list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartEvent(StartEvent event) {

        Log.e("zhangbz", "ListActivity onStartEvent");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStopEvent(StopEvent event) {

        Log.e("zhangbz", "ListActivity onStopEvent");

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {

        Log.e("zhangbz", "ListActivity onErrorEvent");

        Toast.makeText(mContext, "~_~Error~_~", Toast.LENGTH_SHORT).show();
    }
}
