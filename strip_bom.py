import os

csv_dir = r'D:\work2\industry-brain-date'

for f in os.listdir(csv_dir):
    if f.endswith('.csv'):
        filepath = os.path.join(csv_dir, f)
        # Read with utf-8-sig which automatically strips BOM
        with open(filepath, 'r', encoding='utf-8-sig') as fp:
            content = fp.read()
        # Write back without BOM (standard UTF-8)
        with open(filepath, 'w', encoding='utf-8', newline='') as fp:
            fp.write(content)
        print(f' cleaned: {f}')
print('All CSV files cleaned.')
