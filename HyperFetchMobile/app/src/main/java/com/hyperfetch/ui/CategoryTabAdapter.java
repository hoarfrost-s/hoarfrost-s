package com.hyperfetch.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hyperfetch.R;
import com.hyperfetch.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分类 Tab 适配器
 * 用于显示分类标签页，包含分类名称和任务数量徽标
 */
public class CategoryTabAdapter extends RecyclerView.Adapter<CategoryTabAdapter.TabViewHolder> {

    /**
     * 分类列表（包含"全部"选项）
     */
    private List<CategoryItem> categoryItems;

    /**
     * 当前选中的 Tab 位置
     */
    private int selectedPosition = 0;

    /**
     * Tab 点击监听器
     */
    private OnTabClickListener tabClickListener;

    /**
     * 上下文
     */
    private Context context;

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public CategoryTabAdapter(Context context) {
        this.context = context;
        this.categoryItems = new ArrayList<>();

        // 初始化分类列表，添加"全部"选项
        initCategoryItems();
    }

    /**
     * 初始化分类列表项
     */
    private void initCategoryItems() {
        // 添加"全部"分类
        categoryItems.add(new CategoryItem(null, "全部", 0));

        // 添加各个文件分类
        for (Category category : Category.values()) {
            String displayName = UiUtils.getCategoryDisplayName(category);
            categoryItems.add(new CategoryItem(category, displayName, 0));
        }
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_tab, parent, false);
        return new TabViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        CategoryItem item = categoryItems.get(position);
        holder.bindTab(item, position == selectedPosition);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (tabClickListener != null) {
                int oldPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                // 更新旧选中项和新选中项的 UI
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);

                // 回调点击事件
                tabClickListener.onTabClick(item.category, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryItems.size();
    }

    // ==================== 数据更新方法 ====================

    /**
     * 更新分类任务数量
     *
     * @param categoryCounts 各分类的任务数量映射
     */
    public void updateCategoryCounts(Map<Category, Integer> categoryCounts) {
        int totalCount = 0;

        // 更新各分类的数量
        for (int i = 0; i < categoryItems.size(); i++) {
            CategoryItem item = categoryItems.get(i);
            if (item.category == null) {
                // "全部"分类：计算总数
                if (categoryCounts != null) {
                    for (Integer count : categoryCounts.values()) {
                        totalCount += count;
                    }
                    item.taskCount = totalCount;
                }
            } else {
                // 其他分类：从映射中获取数量
                if (categoryCounts != null && categoryCounts.containsKey(item.category)) {
                    item.taskCount = categoryCounts.get(item.category);
                } else {
                    item.taskCount = 0;
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 更新单个分类的任务数量
     *
     * @param category  分类（null 表示全部）
     * @param taskCount 任务数量
     */
    public void updateCategoryCount(Category category, int taskCount) {
        for (int i = 0; i < categoryItems.size(); i++) {
            CategoryItem item = categoryItems.get(i);
            if ((category == null && item.category == null) ||
                    (category != null && category == item.category)) {
                item.taskCount = taskCount;
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * 设置选中的 Tab
     *
     * @param category 分类（null 表示全部）
     */
    public void setSelectedCategory(Category category) {
        for (int i = 0; i < categoryItems.size(); i++) {
            CategoryItem item = categoryItems.get(i);
            if ((category == null && item.category == null) ||
                    (category != null && category == item.category)) {
                int oldPosition = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    /**
     * 获取当前选中的分类
     *
     * @return 当前选中的分类（null 表示全部）
     */
    public Category getSelectedCategory() {
        if (selectedPosition >= 0 && selectedPosition < categoryItems.size()) {
            return categoryItems.get(selectedPosition).category;
        }
        return null;
    }

    /**
     * 获取当前选中的 Tab 位置
     *
     * @return 当前选中的位置
     */
    public int getSelectedPosition() {
        return selectedPosition;
    }

    // ==================== 监听器设置 ====================

    /**
     * 设置 Tab 点击监听器
     *
     * @param listener Tab 点击监听器
     */
    public void setOnTabClickListener(OnTabClickListener listener) {
        this.tabClickListener = listener;
    }

    /**
     * Tab 点击监听器接口
     */
    public interface OnTabClickListener {
        /**
         * Tab 点击回调
         *
         * @param category  选中的分类（null 表示全部）
         * @param position  选中的位置
         */
        void onTabClick(Category category, int position);
    }

    /**
     * Tab ViewHolder
     * 用于绑定分类 Tab 数据到视图
     */
    public static class TabViewHolder extends RecyclerView.ViewHolder {

        /**
         * 分类名称 TextView
         */
        private final TextView textCategoryName;

        /**
         * 任务数量徽标 TextView
         */
        private final TextView badgeTaskCount;

        /**
         * 上下文
         */
        private final Context context;

        /**
         * 构造函数
         *
         * @param itemView 列表项视图
         */
        public TabViewHolder(@NonNull View itemView) {
            super(itemView);
            this.context = itemView.getContext();
            textCategoryName = itemView.findViewById(R.id.text_category_name);
            badgeTaskCount = itemView.findViewById(R.id.badge_task_count);
        }

        /**
         * 绑定 Tab 数据
         *
         * @param item      分类项数据
         * @param isSelected 是否选中
         */
        public void bindTab(CategoryItem item, boolean isSelected) {
            // 设置分类名称
            textCategoryName.setText(item.displayName);

            // 设置任务数量徽标
            if (item.taskCount > 0) {
                badgeTaskCount.setVisibility(View.VISIBLE);
                badgeTaskCount.setText(String.valueOf(item.taskCount));
            } else {
                badgeTaskCount.setVisibility(View.GONE);
            }

            // 设置选中状态样式
            updateSelectedStyle(isSelected);
        }

        /**
         * 更新选中状态样式
         *
         * @param isSelected 是否选中
         */
        private void updateSelectedStyle(boolean isSelected) {
            // 设置文字颜色
            int textColor;
            int badgeColor;

            if (isSelected) {
                textColor = context.getResources().getColor(R.color.colorPrimary, null);
                badgeColor = context.getResources().getColor(R.color.colorPrimary, null);

                // 设置选中背景
                GradientDrawable background = new GradientDrawable();
                background.setShape(GradientDrawable.RECTANGLE);
                background.setCornerRadius(16f);
                background.setColor(context.getResources().getColor(R.color.surface, null));
                background.setStroke(2, textColor);
                itemView.setBackground(background);
            } else {
                textColor = context.getResources().getColor(R.color.textSecondary, null);
                badgeColor = context.getResources().getColor(R.color.textSecondary, null);

                // 设置未选中背景（透明）
                itemView.setBackground(null);
            }

            textCategoryName.setTextColor(textColor);

            // 设置徽标背景颜色
            GradientDrawable badgeBackground = new GradientDrawable();
            badgeBackground.setShape(GradientDrawable.RECTANGLE);
            badgeBackground.setCornerRadius(8f);
            badgeBackground.setColor(badgeColor);
            badgeTaskCount.setBackground(badgeBackground);
        }
    }

    /**
     * 分类项数据类
     */
    private static class CategoryItem {
        /**
         * 分类（null 表示"全部"）
         */
        Category category;

        /**
         * 显示名称
         */
        String displayName;

        /**
         * 任务数量
         */
        int taskCount;

        /**
         * 构造函数
         *
         * @param category     分类
         * @param displayName  显示名称
         * @param taskCount    任务数量
         */
        CategoryItem(Category category, String displayName, int taskCount) {
            this.category = category;
            this.displayName = displayName;
            this.taskCount = taskCount;
        }
    }
}