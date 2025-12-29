export async function load({ params, url }) {
    const get = (param) => url.searchParams.get(param);
    return { uuid: get("uuid"), id: get("id"), button: get("button"), bg: get("bg") };
}
