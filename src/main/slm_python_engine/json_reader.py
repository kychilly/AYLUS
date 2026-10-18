import os
import json

def normalize_string(s):
    """Utility to normalize strings for case-insensitive and whitespace-flexible comparison."""
    if not s:
        return ""
    return " ".join(s.strip().lower().replace("_", " ").replace(",", "").split())

def find_branch_file(branch_query, json_dir="../../../json_data_storage"):
    """
    Finds the matching JSON file in json_data_storage/ given a branch query string.
    Handles filename cleaning (e.g., 'Albany, NY' -> 'Albany__NY_Branch.json').
    """
    if not os.path.exists(json_dir):
        # Fallback path if executed from project root
        json_dir = "json_data_storage"

    if not os.path.exists(json_dir):
        return None, f"Error: Storage folder '{json_dir}' not found."

    normalized_query = normalize_string(branch_query)

    for filename in os.listdir(json_dir):
        if filename.endswith(".json"):
            # Clean filename to match against user query
            clean_name = filename.replace(".json", "").replace("__", " ").replace("_Branch", "").replace("_", " ")
            normalized_file = normalize_string(clean_name)

            # Match if query is contained in filename or vice versa
            if normalized_query in normalized_file or normalized_file in normalized_query:
                return os.path.join(json_dir, filename), None

    return None, f"Error: Branch file for '{branch_query}' could not be located in json_data_storage."

def get_volunteer_field(name, branch, field="totalHours", json_dir="../../../json_data_storage"):
    """
    Performs deterministic key lookup inside the targeted branch JSON file.
    Returns exact numerical values or structured entry lists with zero hallucination.
    """
    file_path, err = find_branch_file(branch, json_dir)
    if err:
        return err

    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        return f"Error reading branch JSON file: {str(e)}"

    normalized_target_name = normalize_string(name)
    matched_record = None

    # Case-insensitive lookup over top-level volunteer keys and userId/username fields
    for key, record in data.items():
        user_id = record.get("userId", "") if isinstance(record, dict) else ""
        username = record.get("username", "") if isinstance(record, dict) else ""

        if (normalize_string(key) == normalized_target_name or
            normalize_string(user_id) == normalized_target_name or
            normalize_string(username) == normalized_target_name):
            matched_record = record
            break

    if not matched_record:
        return f"Volunteer '{name}' was not found in the {branch} branch records."

    # Retrieve target field
    if field in matched_record:
        return matched_record[field]
    else:
        return f"Field '{field}' was not found for volunteer '{name}'."

if __name__ == "__main__":
    # Quick standalone test
    test_branch = "Albany"
    test_name = "Ellie Tandlmayer"
    test_field = "totalHours"

    result = get_volunteer_field(test_name, test_branch, test_field)
    print(f"Lookup Test Result: {test_name} ({test_branch} -> {test_field}) = {result}")