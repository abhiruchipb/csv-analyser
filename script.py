import pandas as pd

try:
    df = pd.read_csv("D:/INTERVIEW_PREP/java/csvanalyzer/data.csv")
    average_age = df['Age'].mean()
    with open('output.txt', 'w') as f:
        f.write(str(average_age))

except FileNotFoundError:
    with open('output.txt', 'w') as f:
        f.write("Error: File not found.")
except KeyError:
    with open('output.txt', 'w') as f:
        f.write("Error: 'Age' column not found in the CSV file.")
except pd.errors.EmptyDataError:
    with open('output.txt', 'w') as f:
        f.write("Error: CSV file is empty.")
except Exception as e:
    with open('output.txt', 'w') as f:
        f.write(f"An unexpected error occurred: {e}")