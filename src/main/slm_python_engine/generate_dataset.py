import os
import json
import random

# System prompt defining the SLM's identity and output format
SYSTEM_PROMPT = (
    "You are an AI assistant for the AYLUS volunteer management system. "
    "Your sole task is to convert natural language queries into a structured JSON tool call "
    "with the format: {\"action\": \"get_volunteer_data\", \"parameters\": {\"name\": \"...\", \"branch\": \"...\", \"field\": \"...\"}}. "
    "Allowed fields are 'totalHours', 'totalMoneyOwed', 'entries', or 'paymentHistory'."
)

# Natural language phrasing variations
HOURS_TEMPLATES = [
    "How many total hours does {name} have in the {branch} branch?",
    "Lookup hours for {name} at {branch}.",
    "Can you check total hours for {name} in {branch}?",
    "What are {name}'s volunteer hours for {branch}?",
    "How many hours has {name} accrued in {branch}?",
    "Get hours for {name} in {branch} branch."
]

MONEY_TEMPLATES = [
    "Does {name} owe any money to the {branch} branch?",
    "Check total money owed for {name} in {branch}.",
    "How much does {name} owe at {branch}?",
    "What is the balance for {name} in {branch}?"
]

ENTRIES_TEMPLATES = [
    "Show me the event entries for {name} in {branch}.",
    "What events did {name} attend at {branch}?",
    "List volunteer log entries for {name} in {branch} branch."
]


def clean_branch_name(filename):
    """
    Cleans filenames like 'Albany__NY_Branch.json' or 'Arlington__TX_.json'
    into human-readable branch names like 'Albany, NY' or 'Arlington, TX'.
    """
    base = filename.replace(".json", "")
    # Standardize branch naming for prompts
    base = base.replace("__", ", ").replace("_Branch", "").replace("_", " ").strip()
    return base


def load_real_volunteers_and_branches(json_dir="../../../json_data_storage"):
    """
    Scans json_data_storage/ and extracts real volunteer names mapped to their respective branch.
    Returns a dictionary: {cleaned_branch_name: [list_of_real_volunteer_names]}
    """
    branch_data = {}

    # Check fallback path relative to src/main/slm_python_engine/
    if not os.path.exists(json_dir):
        # Alternative path check if script is executed from project root
        json_dir = "json_data_storage"

    if not os.path.exists(json_dir):
        raise FileNotFoundError(f"Could not locate json_data_storage folder at '{json_dir}'.")

    for file in os.listdir(json_dir):
        if file.endswith(".json"):
            file_path = os.path.join(json_dir, file)
            branch_display_name = clean_branch_name(file)

            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    data = json.load(f)

                # Extract real volunteer top-level keys
                if isinstance(data, dict):
                    real_names = list(data.keys())
                    if real_names:
                        branch_data[branch_display_name] = real_names
            except Exception as e:
                print(f"Skipping {file} due to error: {e}")

    return branch_data


def create_chatml_example(user_query, target_params):
    """Formats prompt and response into ChatML structure."""
    return {
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_query},
            {
                "role": "assistant",
                "content": json.dumps({
                    "action": "get_volunteer_data",
                    "parameters": target_params
                })
            }
        ]
    }


def generate_dataset(output_file="dataset.jsonl", total_samples=500):
    branch_data = load_real_volunteers_and_branches()
    branches = list(branch_data.keys())

    if not branches:
        print("No branch files found in json_data_storage.")
        return

    dataset = []

    for _ in range(total_samples):
        # Pick a real branch that has at least one volunteer
        branch = random.choice(branches)
        real_name = random.choice(branch_data[branch])

        # Pick query type with weighted distribution (mostly hours)
        query_type = random.choices(
            ["totalHours", "totalMoneyOwed", "entries"],
            weights=[0.70, 0.15, 0.15]
        )[0]

        if query_type == "totalHours":
            template = random.choice(HOURS_TEMPLATES)
        elif query_type == "totalMoneyOwed":
            template = random.choice(MONEY_TEMPLATES)
        else:
            template = random.choice(ENTRIES_TEMPLATES)

        user_text = template.format(name=real_name, branch=branch)
        target_params = {
            "name": real_name,
            "branch": branch,
            "field": query_type
        }

        dataset.append(create_chatml_example(user_text, target_params))

    # Save dataset directly inside src/main/slm_python_engine/dataset.jsonl
    with open(output_file, "w", encoding="utf-8") as f:
        for entry in dataset:
            f.write(json.dumps(entry) + "\n")

    print(f"Successfully generated {len(dataset)} instruction pairs in '{output_file}' using real volunteer records.")


if __name__ == "__main__":
    generate_dataset()