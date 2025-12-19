import requests
import argparse
import json
import sys
import os
import io
import time
from datetime import datetime, timedelta

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')


class XinfadiCrawler:
    def __init__(self, result_dir="result"):
        self.base_url = "http://www.xinfadi.com.cn/getPriceData.html"
        self.session = requests.Session()
        self.result_dir = result_dir
        # 创建结果目录
        os.makedirs(self.result_dir, exist_ok=True)
        # 设置请求头，模拟浏览器访问
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
            'Referer': 'http://www.xinfadi.com.cn/priceDetail.html',
            'X-Requested-With': 'XMLHttpRequest'
        })

    def get_price_data(self, current=1, limit=20, pubDateStartTime=None,
                       pubDateEndTime=None, prodPcatid=None, prodCatid=None,
                       prodName=None):
        """
        获取农产品价格数据

        参数说明:
        - current: 当前页码
        - limit: 每页显示数量
        - pubDateStartTime: 发布开始时间
        - pubDateEndTime: 发布结束时间
        - prodPcatid: 一级分类ID
        - prodCatid: 二级分类ID
        - prodName: 商品名称
        """

        # 构造请求参数
        params = {
            'limit': limit,
            'current': current
        }

        # 添加可选参数
        if pubDateStartTime:
            params['pubDateStartTime'] = pubDateStartTime
        if pubDateEndTime:
            params['pubDateEndTime'] = pubDateEndTime
        if prodPcatid:
            params['prodPcatid'] = prodPcatid
        if prodCatid:
            params['prodCatid'] = prodCatid
        if prodName:
            params['prodName'] = prodName

        try:
            response = self.session.post(self.base_url, data=params)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"请求失败: {e}")
            return None

    def get_limited_price_data(self, max_pages=10, limit=20, pubDateStartTime=None,
                               pubDateEndTime=None, prodPcatid=None, prodCatid=None,
                               prodName=None):
        """
        获取限定页数的价格数据

        参数说明:
        - max_pages: 最大获取页数，默认10页
        - limit: 每页显示数量
        - 其他参数同 get_price_data 方法
        """
        print(f"开始获取数据，最多获取 {max_pages} 页...")

        all_data = []

        # 获取指定页数的数据
        for page in range(1, max_pages + 1):
            print(f"正在获取第 {page} 页...")
            # 添加延时避免请求过于频繁
            time.sleep(0.5)

            page_data = self.get_price_data(
                current=page, limit=limit, pubDateStartTime=pubDateStartTime,
                pubDateEndTime=pubDateEndTime, prodPcatid=prodPcatid,
                prodCatid=prodCatid, prodName=prodName
            )

            if page_data and 'list' in page_data:
                all_data.extend(page_data['list'])
                print(f"已获取第 {page} 页，共 {len(page_data['list'])} 条记录")
            else:
                print(f"第 {page} 页获取失败，停止获取")
                break

        # 构造完整数据结构
        complete_data = {
            'current': 1,
            'limit': len(all_data),
            'count': len(all_data),
            'list': all_data
        }

        print(f"数据获取完成，总共获取 {len(all_data)} 条记录")
        return complete_data

    def save_to_json(self, data, filename):
        """将数据保存为JSON格式"""
        filepath = os.path.join(self.result_dir, filename)
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"数据已保存至: {filepath}")

    def save_to_csv(self, data, product_name):
        """将数据保存为CSV格式"""
        # 生成安全的文件名
        safe_product_name = "".join(c for c in product_name if c.isalnum() or c in (' ', '-', '_')).rstrip()
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"{safe_product_name}_price_data_{timestamp}.csv"
        filepath = os.path.join(self.result_dir, filename)

        import csv
        if data and 'list' in data and data['list']:
            with open(filepath, 'w', newline='', encoding='utf-8-sig') as f:
                writer = csv.writer(f)
                # 写入表头
                headers = ['一级分类', '二级分类', '品名', '最低价', '平均价', '最高价', '规格', '产地', '单位', '发布日期']
                writer.writerow(headers)
                # 写入数据
                for item in data['list']:
                    row = [
                        item.get('prodCat', ''),
                        item.get('prodPcat', ''),
                        item.get('prodName', ''),
                        item.get('lowPrice', ''),
                        item.get('avgPrice', ''),
                        item.get('highPrice', ''),
                        item.get('specInfo', ''),
                        item.get('place', ''),
                        item.get('unitInfo', ''),
                        item.get('pubDate', '')
                    ]
                    writer.writerow(row)
            print(f"数据已保存至: {filepath}")

        # 返回文件名供外部使用
        return filename

    def get_all_categories(self):
        """
        获取所有分类信息（需要根据实际接口调整）
        """
        # 从前端代码可以看到的分类信息
        categories = {
            "一级分类": {
                "蔬菜": 1186,
                "水果": 1187,
                "肉禽蛋": 1189,
                "水产": 1190,
                "粮油": 1188,
                "豆制品": 1203,
                "调料": 1204
            }
        }
        return categories

def main():
    crawler = XinfadiCrawler(result_dir="result")

    # 获取当前日期
    today = datetime.now().strftime("%Y-%m-%d")
    # 获取一年前的日期
    one_year_ago = (datetime.now() - timedelta(days=365)).strftime("%Y-%m-%d")

    # 获取限定页数的数据并保存（默认最多10页）
    # 添加默认搜索条件：时间范围为一年，品类为"苹果"
    print("获取限定页数的价格数据...")
    print(f"搜索条件：时间范围 {one_year_ago} 至 {today}，品类：苹果")

    all_data = crawler.get_limited_price_data(
        max_pages=10,
        limit=100,
        pubDateStartTime=one_year_ago,
        pubDateEndTime=today,
        prodName="苹果"
    )

    if all_data:
        # 保存数据
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        crawler.save_to_csv(all_data, f"apple_price_data_{timestamp}.csv")

        print(f"\n数据概览:")
        print(f"总共获取 {len(all_data['list'])} 条记录")
        if all_data['list']:
            print("前3条数据:")
            for item in all_data['list'][:3]:
                print(f"  品名: {item.get('prodName', '')}, "
                      f"平均价: {item.get('avgPrice', '')}, "
                      f"产地: {item.get('place', '')}")

def main_cli():
    parser = argparse.ArgumentParser(description='农产品价格数据爬虫')
    parser.add_argument('--start_time', required=True, help='查询开始时间，格式为YYYY-MM-DD')
    parser.add_argument('--end_time', required=True, help='查询结束时间，格式为YYYY-MM-DD')
    parser.add_argument('--product_name', required=True, help='查询的农产品品种名称')

    args = parser.parse_args()

    try:
        # 使用传入的参数
        crawler = XinfadiCrawler(result_dir="result")

        # 获取限定页数的数据并保存
        all_data = crawler.get_limited_price_data(
            max_pages=10,
            limit=100,
            pubDateStartTime=args.start_time,
            pubDateEndTime=args.end_time,
            prodName=args.product_name
        )

        # 保存数据到CSV文件
        filename = crawler.save_to_csv(all_data, args.product_name)

        # 输出结果给Java调用方
        result = {
            "file_name": filename
        }
        # 使用ensure_ascii=False确保中文正确输出
        print(json.dumps(result, ensure_ascii=False))
        sys.exit(0)

    except Exception as e:
        error_result = {
            "error": str(e)
        }
        print(json.dumps(error_result))
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) > 1:
        main_cli()
    else:
        main()
