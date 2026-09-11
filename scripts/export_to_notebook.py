import os
import json
import csv
from collections import defaultdict
import matplotlib.pyplot as plt

# 1. Tự động xác định đường dẫn thư mục gốc và thư mục results/
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
RESULTS_DIR = os.path.join(PROJECT_ROOT, "results")
CSV_PATH = os.path.join(RESULTS_DIR, "comparison_results_all.csv")
NOTEBOOK_PATH = os.path.join(RESULTS_DIR, "comparison_report.ipynb")

if not os.path.exists(CSV_PATH):
    print(f"[ERROR] Không tìm thấy file dữ liệu tại:\n{CSV_PATH}")
    exit(1)

# Hàm đổi ngưỡng thành dạng K và M (ví dụ: 500000 -> 500K, 16000000 -> 16M)
def format_threshold(val_str):
    val = int(val_str)
    if val >= 1_000_000:
        return f"{val // 1_000_000}M"
    elif val >= 1_000:
        return f"{val // 1_000}K"
    return str(val)

# Đọc dữ liệu từ CSV
data_by_dataset = defaultdict(lambda: defaultdict(dict))

with open(CSV_PATH, mode="r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        ds = row["Dataset"].lower()
        min_u = int(row["MinUtility"])
        algo = row["Algorithm"]
        time_ms = float(row["ExecutionTime_ms"])
        mem_mb = float(row["Memory_MB"])

        if min_u not in data_by_dataset[ds]:
            data_by_dataset[ds][min_u] = {
                "label": format_threshold(min_u),
                "SUM_time": None, "ULB_time": None,
                "SUM_mem": None, "ULB_mem": None
            }

        if algo == "SUM":
            data_by_dataset[ds][min_u]["SUM_time"] = time_ms
            data_by_dataset[ds][min_u]["SUM_mem"] = mem_mb
        elif algo == "ULB":
            data_by_dataset[ds][min_u]["ULB_time"] = time_ms
            data_by_dataset[ds][min_u]["ULB_mem"] = mem_mb

datasets = ["foodmart", "chess", "connect", "mushroom"]

# 2. VẼ VÀ XUẤT HÌNH ẢNH BIỂU ĐỒ GIỐNG PAPER (Figure 16 & Figure 17)
def plot_paper_graphs(metric_type="time"):
    fig, axes = plt.subplots(2, 2, figsize=(11, 9))
    axes = axes.flatten()

    is_time = (metric_type == "time")
    metric_name = "Execution Time" if is_time else "Peak Memory"
    y_label = "Time (ms)" if is_time else "Memory (MB)"
    key_sum = "SUM_time" if is_time else "SUM_mem"
    key_ulb = "ULB_time" if is_time else "ULB_mem"

    for i, ds in enumerate(datasets):
        ax = axes[i]
        thresholds = sorted(data_by_dataset[ds].keys())
        x_labels = [data_by_dataset[ds][th]["label"] for th in thresholds]

        y_sum = [data_by_dataset[ds][th][key_sum] for th in thresholds]
        y_ulb = [data_by_dataset[ds][th][key_ulb] for th in thresholds]

        # Style đồ thị theo phong cách paper: tam giác ngược, đường nét đứt / liền
        ax.plot(x_labels, y_ulb, color="blue", marker="v", linestyle="-", label="ULB", markersize=7, linewidth=1.5)
        ax.plot(x_labels, y_sum, color="green", marker="v", linestyle="--", label="SUM", markersize=7, linewidth=1.5)

        ax.set_title(f"({chr(97+i)}) {ds}", y=-0.23, fontsize=12, fontweight="medium")
        ax.set_xlabel("Minimum Utility Threshold", fontsize=10)
        ax.set_ylabel(y_label, fontsize=10)
        ax.grid(True, linestyle="--", alpha=0.7, color="gray")

    # Thêm Legend ở phía dưới tương tự Paper
    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="lower center", ncol=2, bbox_to_anchor=(0.5, -0.04), frameon=True, fontsize=11)

    plt.tight_layout()
    plt.subplots_adjust(bottom=0.1)

    img_filename = "figure16_execution_time.png" if is_time else "figure17_peak_memory.png"
    out_path = os.path.join(RESULTS_DIR, img_filename)
    plt.savefig(out_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f">> [GRAPH CẬP NHẬT] Đã tạo: {out_path}")

plot_paper_graphs(metric_type="time")
plot_paper_graphs(metric_type="memory")

# 3. TẠO FILE JUPYTER NOTEBOOK (.ipynb) CHỨA SẴN GRAPH ĐỂ XEM TRỰC TIẾP
notebook_content = {
    "cells": [
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "# 📈 Báo Cáo Biểu Đồ So Sánh: SUM vs. ULB-Miner\n",
                "Tái tạo trực quan các đồ thị từ Figure 16 (Thời gian) và Figure 17 (Bộ nhớ) theo bài báo trên 4 dataset: **foodmart, chess, connect, mushroom**."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "import os\n",
                "from IPython.display import Image, display\n",
                "\n",
                "print('Biểu đồ 1: Figure 16 - Execution Time (ms)')\n",
                "display(Image(filename='figure16_execution_time.png'))\n",
                "\n",
                "print('Biểu đồ 2: Figure 17 - Peak Memory (MB)')\n",
                "display(Image(filename='figure17_peak_memory.png'))"
            ]
        }
    ],
    "metadata": {
        "kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"},
        "language_info": {"name": "python", "version": "3"}
    },
    "nbformat": 4,
    "nbformat_minor": 5
}

with open(NOTEBOOK_PATH, "w", encoding="utf-8") as f:
    json.dump(notebook_content, f, indent=2, ensure_ascii=False)

print(f">> [NOTEBOOK CẬP NHẬT] Đã đóng gói vào: {NOTEBOOK_PATH}")
print("\n[HOÀN TẤT] Bạn có thể kiểm tra ảnh và notebook trong thư mục results/.")